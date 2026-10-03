package com.sriram.shiftmate.signup;

import com.sriram.shiftmate.account.Account;
import com.sriram.shiftmate.auth.CurrentUser;
import com.sriram.shiftmate.common.ConflictException;
import com.sriram.shiftmate.common.NotFoundException;
import com.sriram.shiftmate.notify.Notification;
import com.sriram.shiftmate.notify.Notifier;
import com.sriram.shiftmate.shift.Shift;
import com.sriram.shiftmate.shift.ShiftRepository;
import com.sriram.shiftmate.signup.SignupDtos.RosterEntry;
import com.sriram.shiftmate.signup.SignupDtos.ShiftView;
import com.sriram.shiftmate.signup.SignupDtos.SignupView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class SignupService {

    static final List<SignupStatus> ACTIVE = List.of(SignupStatus.CONFIRMED, SignupStatus.WAITLISTED);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE MMM d, h:mm a");

    private final SignupRepository signups;
    private final ShiftRepository shifts;
    private final CurrentUser currentUser;
    private final Notifier notifier;
    private final Clock clock;
    private final Duration lateCancelWindow;

    public SignupService(SignupRepository signups, ShiftRepository shifts, CurrentUser currentUser,
                         Notifier notifier, Clock clock,
                         @Value("${app.late-cancel-window:PT24H}") Duration lateCancelWindow) {
        this.signups = signups;
        this.shifts = shifts;
        this.currentUser = currentUser;
        this.notifier = notifier;
        this.clock = clock;
        this.lateCancelWindow = lateCancelWindow;
    }

    // ------------------------------------------------------------------ volunteer side

    @Transactional(readOnly = true)
    public List<ShiftView> browse() {
        Long me = currentUser.id();
        return shifts.findAllByCancelledFalseAndStartsAtAfterOrderByStartsAtAsc(clock.instant()).stream()
                .map(s -> view(s, me)).toList();
    }

    public SignupView signUp(Long shiftId) {
        Account me = currentUser.get();
        Shift shift = shifts.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (shift.isCancelled()) {
            throw new IllegalStateException("This shift was cancelled");
        }
        if (!shift.getStartsAt().isAfter(clock.instant())) {
            throw new IllegalStateException("This shift has already started");
        }
        if (signups.findByShiftIdAndVolunteerIdAndStatusIn(shiftId, me.getId(), ACTIVE).isPresent()) {
            throw new ConflictException("You're already signed up for this shift");
        }
        List<Signup> clashes = signups.findConfirmedOverlapping(me.getId(), shift.getStartsAt(), shift.getEndsAt());
        if (!clashes.isEmpty()) {
            Shift other = clashes.get(0).getShift();
            throw new ConflictException("This overlaps your shift \"" + other.getTitle() + "\" on "
                    + WHEN.format(other.getStartsAt().atZone(clock.getZone())));
        }
        long confirmed = signups.countByShiftIdAndStatus(shiftId, SignupStatus.CONFIRMED);
        SignupStatus status = confirmed < shift.getCapacity() ? SignupStatus.CONFIRMED : SignupStatus.WAITLISTED;
        Signup s = signups.save(new Signup(shift, me, status, clock.instant()));
        return toView(s, me.getId());
    }

    public SignupView cancel(Long signupId) {
        Signup s = signups.findById(signupId).orElseThrow(() -> new NotFoundException("Signup not found"));
        if (!Objects.equals(s.getVolunteer().getId(), currentUser.id())) {
            throw new NotFoundException("Signup not found");
        }
        if (!s.getStatus().isActive()) {
            throw new IllegalStateException("This signup is already " + s.getStatus().name().toLowerCase().replace('_', ' '));
        }
        Instant now = clock.instant();
        if (!s.getShift().getStartsAt().isAfter(now)) {
            throw new IllegalStateException("The shift has started; ask the coordinator to update attendance");
        }
        boolean wasConfirmed = s.getStatus() == SignupStatus.CONFIRMED;
        boolean late = wasConfirmed && Duration.between(now, s.getShift().getStartsAt()).compareTo(lateCancelWindow) < 0;
        s.setStatus(late ? SignupStatus.LATE_CANCELLED : SignupStatus.CANCELLED);
        s.setCancelledAt(now);
        if (wasConfirmed) {
            promoteNext(s.getShift());
        }
        return toView(s, s.getVolunteer().getId());
    }

    @Transactional(readOnly = true)
    public List<SignupView> mine() {
        Long me = currentUser.id();
        return signups.findAllByVolunteerIdOrderByCreatedAtDesc(me).stream()
                .sorted(Comparator.comparing((Signup x) -> x.getShift().getStartsAt()))
                .map(s -> toView(s, me)).toList();
    }

    /** First person on the waitlist gets the seat and is told immediately. */
    void promoteNext(Shift shift) {
        if (signups.countByShiftIdAndStatus(shift.getId(), SignupStatus.CONFIRMED) >= shift.getCapacity()) {
            return;
        }
        signups.findAllByShiftIdAndStatusOrderByCreatedAtAsc(shift.getId(), SignupStatus.WAITLISTED).stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setStatus(SignupStatus.CONFIRMED);
                    next.setPromotedAt(clock.instant());
                    notifier.send(next.getVolunteer(), Notification.Type.PROMOTED_FROM_WAITLIST,
                            "A spot opened up: you're now confirmed for \"" + shift.getTitle() + "\" on "
                                    + WHEN.format(shift.getStartsAt().atZone(clock.getZone())) + ".", shift.getId());
                });
    }

    // ---------------------------------------------------------------- coordinator side

    @Transactional(readOnly = true)
    public List<RosterEntry> roster(Long shiftId) {
        Shift shift = ownedShift(shiftId);
        List<Signup> all = signups.findAllByShiftIdOrderByCreatedAtAsc(shift.getId());
        Map<Long, Integer> waitPos = waitlistPositions(all);
        return all.stream().map(s -> {
            Account v = s.getVolunteer();
            int reliability = Reliability.of(signups.findAllByVolunteerIdOrderByCreatedAtDesc(v.getId())).score();
            return new RosterEntry(s.getId(), v.getId(), v.getName(), v.getEmail(), v.getPhone(), s.getStatus(),
                    reliability, waitPos.get(s.getId()));
        }).toList();
    }

    public RosterEntry markAttendance(Long signupId, boolean attended) {
        Signup s = signups.findById(signupId).orElseThrow(() -> new NotFoundException("Signup not found"));
        ownedShift(s.getShift().getId());
        if (s.getShift().getStartsAt().isAfter(clock.instant())) {
            throw new IllegalStateException("You can record attendance once the shift has started");
        }
        if (!s.getStatus().holdsSeat()) {
            throw new IllegalStateException("Only confirmed volunteers can be checked in");
        }
        s.setStatus(attended ? SignupStatus.ATTENDED : SignupStatus.NO_SHOW);
        s.setCheckedInAt(clock.instant());
        s.setHoursCredited(attended ? Math.round(s.getShift().hours() * 100) / 100.0 : 0);
        Account v = s.getVolunteer();
        return new RosterEntry(s.getId(), v.getId(), v.getName(), v.getEmail(), v.getPhone(), s.getStatus(),
                Reliability.of(signups.findAllByVolunteerIdOrderByCreatedAtDesc(v.getId())).score(), null);
    }

    Shift ownedShift(Long shiftId) {
        Shift shift = shifts.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (!Objects.equals(shift.getCoordinator().getId(), currentUser.id())) {
            throw new AccessDeniedException("Not your shift");
        }
        return shift;
    }

    // ------------------------------------------------------------------------ helpers

    ShiftView view(Shift s, Long volunteerId) {
        List<Signup> all = signups.findAllByShiftIdOrderByCreatedAtAsc(s.getId());
        long confirmed = all.stream().filter(x -> x.getStatus() == SignupStatus.CONFIRMED
                || x.getStatus() == SignupStatus.ATTENDED || x.getStatus() == SignupStatus.NO_SHOW).count();
        long waitlisted = all.stream().filter(x -> x.getStatus() == SignupStatus.WAITLISTED).count();
        Signup mine = all.stream().filter(x -> Objects.equals(x.getVolunteer().getId(), volunteerId))
                .filter(x -> x.getStatus() != SignupStatus.CANCELLED).reduce((a, b) -> b).orElse(null);
        Integer pos = mine == null ? null : waitlistPositions(all).get(mine.getId());
        return ShiftView.of(s, confirmed, waitlisted, mine, pos);
    }

    private SignupView toView(Signup s, Long volunteerId) {
        ShiftView sv = view(s.getShift(), volunteerId);
        return new SignupView(s.getId(), sv, s.getStatus(), s.getCreatedAt(), s.getCancelledAt(), s.getCheckedInAt(),
                s.getHoursCredited(), sv.myWaitlistPosition());
    }

    private static Map<Long, Integer> waitlistPositions(List<Signup> allForShift) {
        Map<Long, Integer> pos = new HashMap<>();
        int i = 1;
        for (Signup s : allForShift) {
            if (s.getStatus() == SignupStatus.WAITLISTED) {
                pos.put(s.getId(), i++);
            }
        }
        return pos;
    }

}

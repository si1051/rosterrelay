package com.sriram.shiftmate.shift;

import com.sriram.shiftmate.account.Account;
import com.sriram.shiftmate.auth.CurrentUser;
import com.sriram.shiftmate.notify.Notification;
import com.sriram.shiftmate.notify.Notifier;
import com.sriram.shiftmate.shift.ShiftDtos.ShiftRequest;
import com.sriram.shiftmate.signup.SignupDtos.ShiftView;
import com.sriram.shiftmate.signup.Signup;
import com.sriram.shiftmate.signup.SignupRepository;
import com.sriram.shiftmate.signup.SignupStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ShiftService {

    private final ShiftRepository shifts;
    private final SignupRepository signups;
    private final CurrentUser currentUser;
    private final Notifier notifier;
    private final Clock clock;

    public ShiftService(ShiftRepository shifts, SignupRepository signups, CurrentUser currentUser, Notifier notifier,
                        Clock clock) {
        this.shifts = shifts;
        this.signups = signups;
        this.currentUser = currentUser;
        this.notifier = notifier;
        this.clock = clock;
    }

    public Shift create(ShiftRequest req) {
        Account me = currentUser.get();
        Shift s = new Shift(me);
        s.setTitle(req.title().trim());
        s.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        s.setLocation(req.location().trim());
        s.setStartsAt(req.startsAt());
        s.setEndsAt(req.endsAt());
        s.setCapacity(req.capacity());
        return shifts.save(s);
    }

    @Transactional(readOnly = true)
    public List<ShiftView> mine() {
        return shifts.findAllByCoordinatorIdOrderByStartsAtDesc(currentUser.id()).stream()
                .map(s -> {
                    List<Signup> all = signups.findAllByShiftIdOrderByCreatedAtAsc(s.getId());
                    long confirmed = all.stream().filter(x -> x.getStatus().holdsSeat()).count();
                    long waitlisted = all.stream().filter(x -> x.getStatus() == SignupStatus.WAITLISTED).count();
                    return ShiftView.of(s, confirmed, waitlisted, null, null);
                }).toList();
    }

    public void cancel(Long id) {
        Shift s = shifts.findById(id).orElseThrow(() -> new com.sriram.shiftmate.common.NotFoundException("Shift not found"));
        if (!Objects.equals(s.getCoordinator().getId(), currentUser.id())) {
            throw new AccessDeniedException("Not your shift");
        }
        if (s.isCancelled()) {
            return;
        }
        if (!s.getStartsAt().isAfter(clock.instant())) {
            throw new IllegalStateException("Shifts that already started can't be cancelled");
        }
        s.setCancelled(true);
        for (Signup signup : signups.findAllByShiftIdOrderByCreatedAtAsc(id)) {
            if (signup.getStatus().isActive()) {
                signup.setStatus(SignupStatus.CANCELLED);
                signup.setCancelledAt(clock.instant());
                notifier.send(signup.getVolunteer(), Notification.Type.SHIFT_CANCELLED,
                        "\"" + s.getTitle() + "\" was cancelled by " + s.getCoordinator().getOrganization()
                                + ". Thank you for signing up.", s.getId());
            }
        }
    }
}

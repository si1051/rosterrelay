package com.sriram.rosterrelay.insights;

import com.sriram.rosterrelay.account.Account;
import com.sriram.rosterrelay.account.AccountRepository;
import com.sriram.rosterrelay.auth.CurrentUser;
import com.sriram.rosterrelay.common.NotFoundException;
import com.sriram.rosterrelay.notify.Notification;
import com.sriram.rosterrelay.notify.Notifier;
import com.sriram.rosterrelay.shift.Shift;
import com.sriram.rosterrelay.shift.ShiftRepository;
import com.sriram.rosterrelay.signup.Reliability;
import com.sriram.rosterrelay.signup.Signup;
import com.sriram.rosterrelay.signup.SignupRepository;
import com.sriram.rosterrelay.signup.SignupStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The coordinator's "will this shift actually be staffed?" view: fill and no-show rates, shifts at risk,
 * and the most reliable past volunteers who are free to fill each gap.
 */
@Service
@Transactional(readOnly = true)
public class CoordinatorInsights {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE MMM d, h:mm a");

    private final ShiftRepository shifts;
    private final SignupRepository signups;
    private final AccountRepository accounts;
    private final CurrentUser currentUser;
    private final Notifier notifier;
    private final Clock clock;
    private final Duration horizon;
    private final double threshold;

    public CoordinatorInsights(ShiftRepository shifts, SignupRepository signups, AccountRepository accounts,
                               CurrentUser currentUser, Notifier notifier, Clock clock,
                               @Value("${app.understaffed.horizon:PT72H}") Duration horizon,
                               @Value("${app.understaffed.fill-threshold:0.6}") double threshold) {
        this.shifts = shifts;
        this.signups = signups;
        this.accounts = accounts;
        this.currentUser = currentUser;
        this.notifier = notifier;
        this.clock = clock;
        this.horizon = horizon;
        this.threshold = threshold;
    }

    public record Suggestion(Long volunteerId, String name, int reliability, int shiftsWithYou) {
    }

    public record AtRisk(Long shiftId, String title, Instant startsAt, int capacity, long confirmed, int gap,
                         int fillPercent, List<Suggestion> suggestions) {
    }

    public record Dashboard(int upcomingShifts, Integer upcomingFillRate, Double noShowRate, Double lateCancelRate,
                            double volunteerHours, long uniqueVolunteers, long waitlistPromotions,
                            List<AtRisk> atRisk) {
    }

    public Dashboard dashboard() {
        Long me = currentUser.id();
        Instant now = clock.instant();
        List<Signup> all = signups.findAllForCoordinator(me);

        List<Shift> upcoming = shifts.findAllByCoordinatorIdOrderByStartsAtDesc(me).stream()
                .filter(s -> !s.isCancelled() && s.getStartsAt().isAfter(now)).toList();
        long seats = upcoming.stream().mapToLong(Shift::getCapacity).sum();
        long filled = upcoming.stream().mapToLong(s -> signups.countByShiftIdAndStatus(s.getId(), SignupStatus.CONFIRMED)).sum();

        long attended = count(all, SignupStatus.ATTENDED);
        long noShows = count(all, SignupStatus.NO_SHOW);
        long late = count(all, SignupStatus.LATE_CANCELLED);
        long confirmedEver = attended + noShows + late + count(all, SignupStatus.CONFIRMED);

        List<AtRisk> atRisk = shifts
                .findAllByCoordinatorIdAndCancelledFalseAndStartsAtBetweenOrderByStartsAtAsc(me, now, now.plus(horizon))
                .stream()
                .map(s -> {
                    long c = signups.countByShiftIdAndStatus(s.getId(), SignupStatus.CONFIRMED);
                    return new Object[]{s, c};
                })
                .filter(x -> (long) x[1] < Math.ceil(((Shift) x[0]).getCapacity() * threshold))
                .map(x -> {
                    Shift s = (Shift) x[0];
                    long c = (long) x[1];
                    return new AtRisk(s.getId(), s.getTitle(), s.getStartsAt(), s.getCapacity(), c,
                            (int) (s.getCapacity() - c), (int) Math.round(100.0 * c / s.getCapacity()),
                            suggestions(s, all));
                })
                .toList();

        return new Dashboard(upcoming.size(), seats == 0 ? null : (int) Math.round(100.0 * filled / seats),
                attended + noShows == 0 ? null : round1(100.0 * noShows / (attended + noShows)),
                confirmedEver == 0 ? null : round1(100.0 * late / confirmedEver),
                round1(all.stream().mapToDouble(Signup::getHoursCredited).sum()),
                all.stream().map(s -> s.getVolunteer().getId()).distinct().count(),
                all.stream().filter(s -> s.getPromotedAt() != null).count(),
                atRisk);
    }

    /** Past volunteers of this organization who are free at that time, most reliable first. */
    List<Suggestion> suggestions(Shift shift, List<Signup> orgSignups) {
        Set<Long> alreadyIn = signups.findAllByShiftIdOrderByCreatedAtAsc(shift.getId()).stream()
                .filter(s -> s.getStatus().isActive())
                .map(s -> s.getVolunteer().getId()).collect(Collectors.toSet());
        Map<Account, Long> pool = orgSignups.stream()
                .collect(Collectors.groupingBy(Signup::getVolunteer, Collectors.counting()));
        return pool.entrySet().stream()
                .filter(e -> !alreadyIn.contains(e.getKey().getId()))
                .filter(e -> signups.findConfirmedOverlapping(e.getKey().getId(), shift.getStartsAt(), shift.getEndsAt()).isEmpty())
                .map(e -> new Suggestion(e.getKey().getId(), e.getKey().getName(),
                        Reliability.of(signups.findAllByVolunteerIdOrderByCreatedAtDesc(e.getKey().getId())).score(),
                        e.getValue().intValue()))
                .sorted(Comparator.comparingInt(Suggestion::reliability).reversed()
                        .thenComparing(Suggestion::shiftsWithYou, Comparator.reverseOrder()))
                .limit(5)
                .toList();
    }

    @Transactional
    public void invite(Long shiftId, Long volunteerId) {
        Shift s = shifts.findById(shiftId).orElseThrow(() -> new NotFoundException("Shift not found"));
        if (!Objects.equals(s.getCoordinator().getId(), currentUser.id())) {
            throw new AccessDeniedException("Not your shift");
        }
        Account v = accounts.findById(volunteerId).orElseThrow(() -> new NotFoundException("Volunteer not found"));
        notifier.send(v, Notification.Type.INVITE, s.getCoordinator().getOrganization() + " needs help: \""
                + s.getTitle() + "\" on " + WHEN.format(s.getStartsAt().atZone(clock.getZone()))
                + ". Open spots are first come, first served.", s.getId());
    }

    private static long count(List<Signup> all, SignupStatus st) {
        return all.stream().filter(s -> s.getStatus() == st).count();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

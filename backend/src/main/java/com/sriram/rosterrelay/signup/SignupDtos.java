package com.sriram.rosterrelay.signup;

import com.sriram.rosterrelay.shift.Shift;

import java.time.Instant;

public final class SignupDtos {

    private SignupDtos() {
    }

    public record ShiftView(Long id, String title, String description, String location, String organization,
                            Instant startsAt, Instant endsAt, double hours, int capacity, long confirmed,
                            long waitlisted, boolean cancelled, String myStatus, Long mySignupId,
                            Integer myWaitlistPosition) {

        public static ShiftView of(Shift s, long confirmed, long waitlisted, Signup mine, Integer waitPos) {
            return new ShiftView(s.getId(), s.getTitle(), s.getDescription(), s.getLocation(),
                    s.getCoordinator().getOrganization(), s.getStartsAt(), s.getEndsAt(),
                    Math.round(s.hours() * 100) / 100.0, s.getCapacity(), confirmed, waitlisted, s.isCancelled(),
                    mine == null ? null : mine.getStatus().name(), mine == null ? null : mine.getId(), waitPos);
        }
    }

    public record SignupView(Long id, ShiftView shift, SignupStatus status, Instant createdAt, Instant cancelledAt,
                             Instant checkedInAt, double hoursCredited, Integer waitlistPosition) {
    }

    public record RosterEntry(Long signupId, Long volunteerId, String name, String email, String phone,
                              SignupStatus status, int reliability, Integer waitlistPosition) {
    }
}

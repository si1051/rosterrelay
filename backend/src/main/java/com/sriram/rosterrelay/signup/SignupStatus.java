package com.sriram.rosterrelay.signup;

public enum SignupStatus {
    CONFIRMED,
    WAITLISTED,
    /** Cancelled with enough notice. */
    CANCELLED,
    /** Cancelled inside the late-cancel window: counts against reliability. */
    LATE_CANCELLED,
    ATTENDED,
    NO_SHOW;

    public boolean holdsSeat() {
        return this == CONFIRMED || this == ATTENDED || this == NO_SHOW;
    }

    public boolean isActive() {
        return this == CONFIRMED || this == WAITLISTED;
    }
}

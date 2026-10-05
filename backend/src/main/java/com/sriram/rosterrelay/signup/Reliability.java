package com.sriram.rosterrelay.signup;

import java.util.Collection;

/**
 * Reliability = how likely a volunteer is to show up, with a Bayesian prior so a newcomer with
 * one missed shift isn't branded unreliable forever. Late cancels count as half a no-show.
 */
public record Reliability(int attended, int noShows, int lateCancels, int score) {

    static final double PRIOR_SHOWS = 2.0;
    static final double PRIOR_TOTAL = 2.5;

    public static Reliability of(Collection<Signup> signups) {
        int attended = 0;
        int noShows = 0;
        int late = 0;
        for (Signup s : signups) {
            switch (s.getStatus()) {
                case ATTENDED -> attended++;
                case NO_SHOW -> noShows++;
                case LATE_CANCELLED -> late++;
                default -> {
                }
            }
        }
        double p = (attended + PRIOR_SHOWS) / (attended + noShows + 0.5 * late + PRIOR_TOTAL);
        return new Reliability(attended, noShows, late, (int) Math.round(Math.min(1.0, p) * 100));
    }
}

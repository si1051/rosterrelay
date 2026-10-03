package com.sriram.shiftmate.auth;

import com.sriram.shiftmate.common.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an email out after too many failed logins in a sliding window,
 * slowing down credential-stuffing and brute-force attempts.
 */
@Component
public class LoginRateLimiter {

    private record Attempts(int failures, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public LoginRateLimiter(@Value("${app.security.max-login-failures:5}") int maxFailures,
                            @Value("${app.security.lockout-window:PT15M}") Duration window) {
        this(maxFailures, window, Clock.systemUTC());
    }

    LoginRateLimiter(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public void checkAllowed(String email) {
        Attempts a = attempts.get(key(email));
        if (a == null) {
            return;
        }
        Instant now = clock.instant();
        Instant unlockAt = a.windowStart().plus(window);
        if (now.isAfter(unlockAt)) {
            attempts.remove(key(email));
        } else if (a.failures() >= maxFailures) {
            throw new TooManyRequestsException("Too many failed login attempts. Try again later.",
                    Duration.between(now, unlockAt).toSeconds() + 1);
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attempts.merge(key(email), new Attempts(1, now), (old, n) ->
                now.isAfter(old.windowStart().plus(window)) ? n : new Attempts(old.failures() + 1, old.windowStart()));
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

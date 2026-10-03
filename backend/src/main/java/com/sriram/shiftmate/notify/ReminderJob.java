package com.sriram.shiftmate.notify;

import com.sriram.shiftmate.signup.Signup;
import com.sriram.shiftmate.signup.SignupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** Reminds confirmed volunteers ahead of their shift; each signup is reminded once. */
@Component
public class ReminderJob {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE h:mm a");

    private final SignupRepository signups;
    private final Notifier notifier;
    private final Clock clock;
    private final Duration lead;

    public ReminderJob(SignupRepository signups, Notifier notifier, Clock clock,
                       @Value("${app.reminder-lead:PT24H}") Duration lead) {
        this.signups = signups;
        this.notifier = notifier;
        this.clock = clock;
        this.lead = lead;
    }

    @Scheduled(fixedDelayString = "${app.reminder-interval:PT15M}")
    @Transactional
    public int sendDue() {
        Instant now = clock.instant();
        int sent = 0;
        for (Signup s : signups.findDueForReminder(now, now.plus(lead))) {
            notifier.send(s.getVolunteer(), Notification.Type.REMINDER,
                    "Reminder: \"" + s.getShift().getTitle() + "\" at " + s.getShift().getLocation() + ", "
                            + WHEN.format(s.getShift().getStartsAt().atZone(clock.getZone()))
                            + ". Can't make it? Cancel early so someone on the waitlist gets your spot.",
                    s.getShift().getId());
            s.setRemindedAt(now);
            sent++;
        }
        return sent;
    }
}

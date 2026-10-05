package com.sriram.rosterrelay.notify;

import com.sriram.rosterrelay.account.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Stores an in-app notification and hands it to the outbound channel. The channel here is a log line;
 * swap in an email (SMTP) or SMS (Twilio) sender behind this one method for production.
 */
@Component
public class Notifier {

    private static final Logger log = LoggerFactory.getLogger(Notifier.class);

    private final NotificationRepository notifications;
    private final Clock clock;

    public Notifier(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void send(Account to, Notification.Type type, String message, Long shiftId) {
        notifications.save(new Notification(to, type, message, shiftId, clock.instant()));
        log.info("notify account={} type={} shift={} msg=\"{}\"", to.getId(), type, shiftId, message);
    }
}

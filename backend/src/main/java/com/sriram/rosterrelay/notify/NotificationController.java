package com.sriram.rosterrelay.notify;

import com.sriram.rosterrelay.auth.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final NotificationRepository notifications;
    private final CurrentUser currentUser;

    public NotificationController(NotificationRepository notifications, CurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    public record NotificationView(Long id, Notification.Type type, String message, Long shiftId, boolean read,
                                   Instant createdAt) {
    }

    public record Inbox(long unread, List<NotificationView> items) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Inbox inbox() {
        Long me = currentUser.id();
        return new Inbox(notifications.countByRecipientIdAndReadFlagFalse(me),
                notifications.findTop50ByRecipientIdOrderByCreatedAtDesc(me).stream()
                        .map(n -> new NotificationView(n.getId(), n.getType(), n.getMessage(), n.getShiftId(),
                                n.isRead(), n.getCreatedAt()))
                        .toList());
    }

    @PostMapping("/read")
    @Transactional
    public void markAllRead() {
        notifications.findAllByRecipientIdAndReadFlagFalse(currentUser.id()).forEach(Notification::markRead);
    }
}

package com.sriram.rosterrelay.notify;

import com.sriram.rosterrelay.account.Account;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_recipient", columnList = "recipient_id,createdAt"))
public class Notification {

    public enum Type { PROMOTED_FROM_WAITLIST, REMINDER, SHIFT_CANCELLED, INVITE, SPOT_OPENED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Account recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Type type;

    @Column(nullable = false, length = 500)
    private String message;

    private Long shiftId;

    private boolean readFlag;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(Account recipient, Type type, String message, Long shiftId, Instant createdAt) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.shiftId = shiftId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public Long getShiftId() { return shiftId; }
    public boolean isRead() { return readFlag; }
    public void markRead() { this.readFlag = true; }
    public Instant getCreatedAt() { return createdAt; }
}

package com.sriram.rosterrelay.signup;

import com.sriram.rosterrelay.account.Account;
import com.sriram.rosterrelay.shift.Shift;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "signups", indexes = {
        @Index(name = "idx_signup_shift_status", columnList = "shift_id,status"),
        @Index(name = "idx_signup_volunteer", columnList = "volunteer_id")
})
public class Signup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Account volunteer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SignupStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant cancelledAt;
    private Instant promotedAt;
    private Instant checkedInAt;
    private Instant remindedAt;
    private double hoursCredited;

    protected Signup() {
    }

    public Signup(Shift shift, Account volunteer, SignupStatus status, Instant createdAt) {
        this.shift = shift;
        this.volunteer = volunteer;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Shift getShift() { return shift; }
    public Account getVolunteer() { return volunteer; }
    public SignupStatus getStatus() { return status; }
    public void setStatus(SignupStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getPromotedAt() { return promotedAt; }
    public void setPromotedAt(Instant promotedAt) { this.promotedAt = promotedAt; }
    public Instant getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }
    public Instant getRemindedAt() { return remindedAt; }
    public void setRemindedAt(Instant remindedAt) { this.remindedAt = remindedAt; }
    public double getHoursCredited() { return hoursCredited; }
    public void setHoursCredited(double hoursCredited) { this.hoursCredited = hoursCredited; }
}

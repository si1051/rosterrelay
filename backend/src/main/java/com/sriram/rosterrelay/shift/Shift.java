package com.sriram.rosterrelay.shift;

import com.sriram.rosterrelay.account.Account;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "shifts", indexes = {
        @Index(name = "idx_shift_start", columnList = "startsAt"),
        @Index(name = "idx_shift_coordinator", columnList = "coordinator_id")
})
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "coordinator_id", nullable = false)
    private Account coordinator;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private int capacity;

    private boolean cancelled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected Shift() {
    }

    public Shift(Account coordinator) {
        this.coordinator = coordinator;
    }

    public double hours() {
        return Duration.between(startsAt, endsAt).toMinutes() / 60.0;
    }

    public boolean overlaps(Shift other) {
        return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
    }

    public Long getId() { return id; }
    public Account getCoordinator() { return coordinator; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public Instant getCreatedAt() { return createdAt; }
}

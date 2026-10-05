package com.sriram.rosterrelay.signup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SignupRepository extends JpaRepository<Signup, Long> {

    List<Signup> findAllByShiftIdOrderByCreatedAtAsc(Long shiftId);

    List<Signup> findAllByVolunteerIdOrderByCreatedAtDesc(Long volunteerId);

    Optional<Signup> findByShiftIdAndVolunteerIdAndStatusIn(Long shiftId, Long volunteerId,
                                                            Collection<SignupStatus> statuses);

    long countByShiftIdAndStatus(Long shiftId, SignupStatus status);

    List<Signup> findAllByShiftIdAndStatusOrderByCreatedAtAsc(Long shiftId, SignupStatus status);

    @Query("""
            SELECT s FROM Signup s
            WHERE s.volunteer.id = :volunteerId AND s.status = com.sriram.rosterrelay.signup.SignupStatus.CONFIRMED
              AND s.shift.cancelled = false AND s.shift.startsAt < :end AND s.shift.endsAt > :start
            """)
    List<Signup> findConfirmedOverlapping(@Param("volunteerId") Long volunteerId, @Param("start") Instant start,
                                          @Param("end") Instant end);

    @Query("""
            SELECT s FROM Signup s
            WHERE s.status = com.sriram.rosterrelay.signup.SignupStatus.CONFIRMED AND s.remindedAt IS NULL
              AND s.shift.cancelled = false AND s.shift.startsAt > :from AND s.shift.startsAt <= :to
            """)
    List<Signup> findDueForReminder(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT s FROM Signup s WHERE s.shift.coordinator.id = :coordinatorId")
    List<Signup> findAllForCoordinator(@Param("coordinatorId") Long coordinatorId);
}

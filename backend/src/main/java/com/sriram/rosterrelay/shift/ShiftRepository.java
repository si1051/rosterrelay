package com.sriram.rosterrelay.shift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findAllByCancelledFalseAndStartsAtAfterOrderByStartsAtAsc(Instant after);

    List<Shift> findAllByCoordinatorIdOrderByStartsAtDesc(Long coordinatorId);

    List<Shift> findAllByCoordinatorIdAndCancelledFalseAndStartsAtBetweenOrderByStartsAtAsc(Long coordinatorId,
                                                                                          Instant from, Instant to);
}

package com.sriram.shiftmate.notify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFlagFalse(Long recipientId);

    List<Notification> findAllByRecipientIdAndReadFlagFalse(Long recipientId);
}

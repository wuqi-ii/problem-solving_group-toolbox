package cn.edu.teamtoolbox.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    List<NotificationEntity> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId);
    long countByRecipientIdAndReadAtIsNull(String recipientId);
    List<NotificationEntity> findAllByRecipientIdAndReadAtIsNull(String recipientId);
    Optional<NotificationEntity> findByRecipientIdAndEventKey(String recipientId, String eventKey);
}

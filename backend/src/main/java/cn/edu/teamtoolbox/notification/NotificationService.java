package cn.edu.teamtoolbox.notification;

import cn.edu.teamtoolbox.common.error.BusinessException;
import cn.edu.teamtoolbox.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) { this.repository = repository; }

    @Transactional
    public void create(String recipientId, String eventKey, String type, String title, String content, String path) {
        if (repository.findByRecipientIdAndEventKey(recipientId, eventKey).isEmpty()) {
            repository.save(new NotificationEntity(recipientId, eventKey, type, title, content, path));
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(String recipientId) {
        return repository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String recipientId) { return repository.countByRecipientIdAndReadAtIsNull(recipientId); }

    @Transactional
    public void markRead(String id, String recipientId) {
        NotificationEntity entity = repository.findById(id)
                .filter(item -> item.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在"));
        entity.markRead();
    }

    @Transactional
    public void markAllRead(String recipientId) {
        repository.findAllByRecipientIdAndReadAtIsNull(recipientId).forEach(NotificationEntity::markRead);
    }

    private NotificationView toView(NotificationEntity item) {
        return new NotificationView(item.getId(), item.getType(), item.getTitle(), item.getContent(),
                item.getTargetPath(), item.getReadAt() != null, item.getCreatedAt());
    }
}

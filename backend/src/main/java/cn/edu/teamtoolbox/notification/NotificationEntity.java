package cn.edu.teamtoolbox.notification;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class NotificationEntity extends AuditableEntity {
    @Id
    @Column(name = "notification_id", length = 36, nullable = false)
    private String id;
    @Column(name = "recipient_id", length = 36, nullable = false)
    private String recipientId;
    @Column(name = "event_key", length = 160, nullable = false)
    private String eventKey;
    @Column(name = "notification_type", length = 40, nullable = false)
    private String type;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 500)
    private String content;
    @Column(name = "target_path", length = 255)
    private String targetPath;
    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {}

    public NotificationEntity(String recipientId, String eventKey, String type, String title,
                              String content, String targetPath) {
        this.id = UUID.randomUUID().toString();
        this.recipientId = recipientId;
        this.eventKey = eventKey;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetPath = targetPath;
    }

    public String getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTargetPath() { return targetPath; }
    public Instant getReadAt() { return readAt; }
    public void markRead() { if (readAt == null) readAt = Instant.now(); }
}

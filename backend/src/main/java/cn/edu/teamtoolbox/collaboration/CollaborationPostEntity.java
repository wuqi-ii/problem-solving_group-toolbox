package cn.edu.teamtoolbox.collaboration;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "collaboration_post")
public class CollaborationPostEntity extends AuditableEntity {
    @Id
    @Column(name = "post_id", length = 36, nullable = false)
    private String id;
    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;
    @Column(name = "task_id", length = 36)
    private String taskId;
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", length = 30, nullable = false)
    private CollaborationPostType type;
    @Column(length = 2000, nullable = false)
    private String content;
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;
    @Column(nullable = false)
    private boolean pinned;
    @Column(length = 20, nullable = false)
    private String status;

    protected CollaborationPostEntity() {}

    public CollaborationPostEntity(String groupId, String taskId, CollaborationPostType type,
                                   String content, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.taskId = taskId;
        this.type = type;
        this.content = content;
        this.createdBy = createdBy;
        this.pinned = false;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getTaskId() { return taskId; }
    public CollaborationPostType getType() { return type; }
    public String getContent() { return content; }
    public String getCreatedBy() { return createdBy; }
    public boolean isPinned() { return pinned; }
    public String getStatus() { return status; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void delete() { this.status = "DELETED"; }
}

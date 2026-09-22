package cn.edu.teamtoolbox.task;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_item")
public class TaskEntity extends AuditableEntity {
    @Id
    @Column(name = "task_id", length = 36, nullable = false)
    private String id;
    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;
    @Column(name = "created_by", length = 36, nullable = false)
    private String createdBy;
    @Column(name = "due_at")
    private Instant dueAt;

    protected TaskEntity() {
    }

    public TaskEntity(String groupId, String title, String description, TaskPriority priority,
                      String createdBy, Instant dueAt) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = TaskStatus.OPEN;
        this.createdBy = createdBy;
        this.dueAt = dueAt;
    }

    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskPriority getPriority() { return priority; }
    public TaskStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public Instant getDueAt() { return dueAt; }
    public void setStatus(TaskStatus status) { this.status = status; }
}

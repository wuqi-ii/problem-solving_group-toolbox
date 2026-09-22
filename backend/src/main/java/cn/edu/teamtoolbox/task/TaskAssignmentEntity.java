package cn.edu.teamtoolbox.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_assignment")
public class TaskAssignmentEntity {
    @Id
    @Column(name = "assignment_id", length = 36, nullable = false)
    private String id;
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected TaskAssignmentEntity() {
    }

    public TaskAssignmentEntity(String taskId, String userId) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.userId = userId;
        this.status = "ACTIVE";
        this.assignedAt = Instant.now();
    }

    public String getTaskId() { return taskId; }
    public String getUserId() { return userId; }
}

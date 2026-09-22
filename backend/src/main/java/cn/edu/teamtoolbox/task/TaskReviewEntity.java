package cn.edu.teamtoolbox.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_review")
public class TaskReviewEntity {
    @Id
    @Column(name = "review_id", length = 36, nullable = false)
    private String id;
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;
    @Column(name = "submission_id", length = 36, nullable = false)
    private String submissionId;
    @Column(name = "reviewed_by", length = 36, nullable = false)
    private String reviewedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewDecision decision;
    @Column(length = 1000)
    private String comment;
    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected TaskReviewEntity() {
    }

    public TaskReviewEntity(String taskId, String submissionId, String reviewedBy,
                            ReviewDecision decision, String comment) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.submissionId = submissionId;
        this.reviewedBy = reviewedBy;
        this.decision = decision;
        this.comment = comment;
        this.reviewedAt = Instant.now();
    }

    public ReviewDecision getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getReviewedAt() { return reviewedAt; }
}

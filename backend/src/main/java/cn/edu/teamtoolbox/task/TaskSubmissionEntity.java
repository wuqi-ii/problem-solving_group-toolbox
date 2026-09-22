package cn.edu.teamtoolbox.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_submission")
public class TaskSubmissionEntity {
    @Id
    @Column(name = "submission_id", length = 36, nullable = false)
    private String id;
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;
    @Column(name = "submitted_by", length = 36, nullable = false)
    private String submittedBy;
    @Column(name = "version_no", nullable = false)
    private int versionNo;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "attachment_url", length = 512)
    private String attachmentUrl;
    @Column(name = "attachment_file_id", length = 36)
    private String attachmentFileId;
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected TaskSubmissionEntity() {
    }

    public TaskSubmissionEntity(String taskId, String submittedBy, int versionNo, String content, String attachmentFileId) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.submittedBy = submittedBy;
        this.versionNo = versionNo;
        this.content = content;
        this.attachmentFileId = attachmentFileId;
        this.submittedAt = Instant.now();
    }

    public String getId() { return id; }
    public int getVersionNo() { return versionNo; }
    public String getContent() { return content; }
    public String getAttachmentFileId() { return attachmentFileId; }
    public String getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
}

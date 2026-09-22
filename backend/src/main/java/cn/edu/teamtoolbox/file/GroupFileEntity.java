package cn.edu.teamtoolbox.file;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "group_file")
public class GroupFileEntity extends AuditableEntity {
    @Id
    @Column(name = "file_id", length = 36, nullable = false)
    private String id;
    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;
    @Column(name = "content_type", length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "uploaded_by", nullable = false, length = 36)
    private String uploadedBy;
    @Column(nullable = false, length = 20)
    private String status;

    protected GroupFileEntity() {
    }

    public GroupFileEntity(String groupId, String originalName, String storageKey,
                           String contentType, long sizeBytes, String uploadedBy) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.originalName = originalName;
        this.displayName = originalName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getGroupId() { return groupId; }
    public String getOriginalName() { return originalName; }
    public String getDisplayName() { return displayName; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getUploadedBy() { return uploadedBy; }
    public String getStatus() { return status; }
    public void rename(String displayName) { this.displayName = displayName; }
    public void softDelete() { this.status = "DELETED"; }
}

package cn.edu.teamtoolbox.transfer;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "temporary_transfer")
public class TemporaryTransferEntity extends AuditableEntity {
    @Id
    @Column(name = "transfer_id", length = 36, nullable = false)
    private String id;
    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;
    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;
    @Column(name = "storage_key", length = 255, nullable = false, unique = true)
    private String storageKey;
    @Column(name = "content_type", length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "pickup_code_hash", length = 64, nullable = false, unique = true)
    private String pickupCodeHash;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "max_downloads", nullable = false)
    private int maxDownloads;
    @Column(name = "download_count", nullable = false)
    private int downloadCount;

    protected TemporaryTransferEntity() {}

    public TemporaryTransferEntity(String ownerId, String originalName, String storageKey, String contentType,
                                   long sizeBytes, String pickupCodeHash, Instant expiresAt, int maxDownloads) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.originalName = originalName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.pickupCodeHash = pickupCodeHash;
        this.status = "ACTIVE";
        this.expiresAt = expiresAt;
        this.maxDownloads = maxDownloads;
        this.downloadCount = 0;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getOriginalName() { return originalName; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxDownloads() { return maxDownloads; }
    public int getDownloadCount() { return downloadCount; }
    public void cancel() { status = "CANCELLED"; }
    public void expire() { status = "EXPIRED"; }
    public void cleaned() { status = "CLEANED"; }
    public void recordDownload() {
        downloadCount++;
        if (downloadCount >= maxDownloads) status = "EXHAUSTED";
    }
}

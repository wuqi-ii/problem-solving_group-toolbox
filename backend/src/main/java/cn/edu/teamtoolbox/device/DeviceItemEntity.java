package cn.edu.teamtoolbox.device;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "device_item")
public class DeviceItemEntity extends AuditableEntity {
    @Id
    @Column(name = "item_id", length = 36, nullable = false)
    private String id;
    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;
    @Column(name = "item_type", length = 20, nullable = false)
    private String type;
    @Column(name = "text_content", length = 4000)
    private String textContent;
    @Column(name = "file_name", length = 255)
    private String fileName;
    @Column(name = "storage_key", length = 255, unique = true)
    private String storageKey;
    @Column(name = "content_type", length = 120)
    private String contentType;
    @Column(name = "size_bytes")
    private Long sizeBytes;
    @Column(name = "source_device", length = 100)
    private String sourceDevice;
    @Column(nullable = false, length = 20)
    private String status;

    protected DeviceItemEntity() {}

    public static DeviceItemEntity text(String ownerId, String type, String content, String sourceDevice) {
        DeviceItemEntity item = base(ownerId, type, sourceDevice);
        item.textContent = content;
        return item;
    }

    public static DeviceItemEntity file(String ownerId, String fileName, String storageKey, String contentType,
                                        long sizeBytes, String sourceDevice) {
        DeviceItemEntity item = base(ownerId, "FILE", sourceDevice);
        item.fileName = fileName;
        item.storageKey = storageKey;
        item.contentType = contentType;
        item.sizeBytes = sizeBytes;
        return item;
    }

    private static DeviceItemEntity base(String ownerId, String type, String sourceDevice) {
        DeviceItemEntity item = new DeviceItemEntity();
        item.id = UUID.randomUUID().toString();
        item.ownerId = ownerId;
        item.type = type;
        item.sourceDevice = sourceDevice;
        item.status = "ACTIVE";
        return item;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getType() { return type; }
    public String getTextContent() { return textContent; }
    public String getFileName() { return fileName; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSourceDevice() { return sourceDevice; }
    public String getStatus() { return status; }
    public void delete() { status = "DELETED"; }
}

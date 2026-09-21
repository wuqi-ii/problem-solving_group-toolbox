package cn.edu.teamtoolbox.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_invitation")
public class GroupInvitationEntity {
    @Id
    @Column(name = "invitation_id", length = 36, nullable = false)
    private String id;

    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GroupInvitationEntity() {
    }

    public GroupInvitationEntity(String groupId, String code, String createdBy, Instant expiresAt, int maxUses) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.code = code;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.usedCount = 0;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public String getGroupId() { return groupId; }
    public String getCode() { return code; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public String getStatus() { return status; }

    public void useOnce() {
        usedCount++;
        if (usedCount >= maxUses) status = "EXHAUSTED";
    }
}

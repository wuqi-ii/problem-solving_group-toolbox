package cn.edu.teamtoolbox.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permission_grant")
public class PermissionGrantEntity {
    @Id
    @Column(name = "grant_id", length = 36, nullable = false)
    private String id;

    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(nullable = false, length = 40)
    private String permission;

    @Column(name = "granted_by", length = 36, nullable = false)
    private String grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected PermissionGrantEntity() {
    }

    public PermissionGrantEntity(String groupId, String userId, String permission, String grantedBy) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.userId = userId;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public String getPermission() { return permission; }
    public void revoke() { this.revokedAt = Instant.now(); }
}

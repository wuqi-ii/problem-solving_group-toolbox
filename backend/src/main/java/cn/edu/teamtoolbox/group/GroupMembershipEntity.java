package cn.edu.teamtoolbox.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_membership")
public class GroupMembershipEntity {
    @Id
    @Column(name = "membership_id", length = 36, nullable = false)
    private String id;

    @Column(name = "group_id", length = 36, nullable = false)
    private String groupId;

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GroupMembershipEntity() {
    }

    public GroupMembershipEntity(String groupId, String userId, String role) {
        this.id = UUID.randomUUID().toString();
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.status = "ACTIVE";
    }

    @PrePersist
    void onCreate() { joinedAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public String getGroupId() { return groupId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
}

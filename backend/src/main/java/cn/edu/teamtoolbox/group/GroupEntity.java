package cn.edu.teamtoolbox.group;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "team_group")
public class GroupEntity extends AuditableEntity {
    @Id
    @Column(name = "group_id", length = 36, nullable = false)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "leader_id", nullable = false, length = 36)
    private String leaderId;

    @Column(nullable = false, length = 20)
    private String status;

    protected GroupEntity() {
    }

    public GroupEntity(String name, String description, String leaderId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.leaderId = leaderId;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLeaderId() { return leaderId; }
    public String getStatus() { return status; }
}

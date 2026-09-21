package cn.edu.teamtoolbox.user;

import cn.edu.teamtoolbox.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity extends AuditableEntity {
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String account;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(nullable = false, length = 20)
    private String status;

    protected UserEntity() {
    }

    public UserEntity(String account, String passwordHash, String nickname) {
        this.id = UUID.randomUUID().toString();
        this.account = account;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getAccount() { return account; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
}

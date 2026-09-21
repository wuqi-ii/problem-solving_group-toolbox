CREATE TABLE group_invitation (
    invitation_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    code VARCHAR(16) NOT NULL UNIQUE,
    created_by VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    max_uses INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_invitation_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_invitation_creator FOREIGN KEY (created_by) REFERENCES app_user(user_id)
);

CREATE INDEX idx_invitation_group ON group_invitation(group_id, status);

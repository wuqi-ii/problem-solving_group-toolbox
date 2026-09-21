CREATE TABLE app_user (
    user_id VARCHAR(36) PRIMARY KEY,
    account VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    avatar_url VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE team_group (
    group_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    leader_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_group_leader FOREIGN KEY (leader_id) REFERENCES app_user(user_id)
);

CREATE TABLE group_membership (
    membership_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_membership_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_membership_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

CREATE TABLE permission_grant (
    grant_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    permission VARCHAR(40) NOT NULL,
    granted_by VARCHAR(36) NOT NULL,
    granted_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    CONSTRAINT fk_grant_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_grant_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT fk_grant_operator FOREIGN KEY (granted_by) REFERENCES app_user(user_id)
);

CREATE INDEX idx_membership_user ON group_membership(user_id, status);
CREATE INDEX idx_permission_scope ON permission_grant(group_id, user_id, permission, revoked_at);

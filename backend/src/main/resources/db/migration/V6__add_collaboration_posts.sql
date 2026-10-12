CREATE TABLE collaboration_post (
    post_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36),
    post_type VARCHAR(30) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_collaboration_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_collaboration_task FOREIGN KEY (task_id) REFERENCES task_item(task_id),
    CONSTRAINT fk_collaboration_author FOREIGN KEY (created_by) REFERENCES app_user(user_id)
);

CREATE INDEX idx_collaboration_group ON collaboration_post(group_id, post_type, status, created_at);
CREATE INDEX idx_collaboration_task ON collaboration_post(task_id, status, created_at);

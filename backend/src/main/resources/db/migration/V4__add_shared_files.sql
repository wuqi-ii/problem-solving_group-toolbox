CREATE TABLE group_file (
    file_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(120),
    size_bytes BIGINT NOT NULL,
    uploaded_by VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_file_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploaded_by) REFERENCES app_user(user_id)
);

ALTER TABLE task_submission ADD COLUMN attachment_file_id VARCHAR(36);
ALTER TABLE task_submission ADD CONSTRAINT fk_submission_file
    FOREIGN KEY (attachment_file_id) REFERENCES group_file(file_id);

CREATE INDEX idx_group_file_active ON group_file(group_id, status, created_at);

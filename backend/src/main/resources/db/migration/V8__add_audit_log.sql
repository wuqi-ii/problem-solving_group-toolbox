CREATE TABLE audit_log (
    audit_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_status INT NOT NULL,
    client_ip VARCHAR(64),
    occurred_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

CREATE INDEX idx_audit_user_time ON audit_log(user_id, occurred_at);

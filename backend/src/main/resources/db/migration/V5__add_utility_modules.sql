CREATE TABLE temporary_transfer (
    transfer_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(120),
    size_bytes BIGINT NOT NULL,
    pickup_code_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    max_downloads INT NOT NULL,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_transfer_owner FOREIGN KEY (owner_id) REFERENCES app_user(user_id)
);

CREATE INDEX idx_transfer_owner ON temporary_transfer(owner_id, created_at);
CREATE INDEX idx_transfer_expiry ON temporary_transfer(status, expires_at);

CREATE TABLE device_item (
    item_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    text_content VARCHAR(4000),
    file_name VARCHAR(255),
    storage_key VARCHAR(255) UNIQUE,
    content_type VARCHAR(120),
    size_bytes BIGINT,
    source_device VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_device_item_owner FOREIGN KEY (owner_id) REFERENCES app_user(user_id)
);

CREATE INDEX idx_device_item_owner ON device_item(owner_id, status, created_at);

CREATE TABLE notification (
    notification_id VARCHAR(36) PRIMARY KEY,
    recipient_id VARCHAR(36) NOT NULL,
    event_key VARCHAR(160) NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_path VARCHAR(255),
    read_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_notification_event UNIQUE (recipient_id, event_key),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES app_user(user_id)
);

CREATE INDEX idx_notification_recipient ON notification(recipient_id, read_at, created_at);

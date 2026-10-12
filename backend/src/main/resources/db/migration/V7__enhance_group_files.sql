ALTER TABLE group_file ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE group_file ADD COLUMN version_no INT NOT NULL DEFAULT 1;
ALTER TABLE group_file ADD COLUMN deleted_at TIMESTAMP(6);

CREATE INDEX idx_group_file_category ON group_file(group_id, status, category, created_at);
CREATE INDEX idx_group_file_version ON group_file(group_id, original_name, version_no);

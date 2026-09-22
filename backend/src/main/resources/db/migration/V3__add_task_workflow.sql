CREATE TABLE task_item (
    task_id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    due_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_task_group FOREIGN KEY (group_id) REFERENCES team_group(group_id),
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES app_user(user_id)
);

CREATE TABLE task_assignment (
    assignment_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_task_assignee UNIQUE (task_id, user_id),
    CONSTRAINT fk_assignment_task FOREIGN KEY (task_id) REFERENCES task_item(task_id),
    CONSTRAINT fk_assignment_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

CREATE TABLE task_submission (
    submission_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    submitted_by VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    attachment_url VARCHAR(512),
    submitted_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_task_submission_version UNIQUE (task_id, version_no),
    CONSTRAINT fk_submission_task FOREIGN KEY (task_id) REFERENCES task_item(task_id),
    CONSTRAINT fk_submission_user FOREIGN KEY (submitted_by) REFERENCES app_user(user_id)
);

CREATE TABLE task_review (
    review_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    submission_id VARCHAR(36) NOT NULL,
    reviewed_by VARCHAR(36) NOT NULL,
    decision VARCHAR(30) NOT NULL,
    comment VARCHAR(1000),
    reviewed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_review_submission UNIQUE (submission_id),
    CONSTRAINT fk_review_task FOREIGN KEY (task_id) REFERENCES task_item(task_id),
    CONSTRAINT fk_review_submission FOREIGN KEY (submission_id) REFERENCES task_submission(submission_id),
    CONSTRAINT fk_review_user FOREIGN KEY (reviewed_by) REFERENCES app_user(user_id)
);

CREATE INDEX idx_task_group_status ON task_item(group_id, status, due_at);
CREATE INDEX idx_assignment_user ON task_assignment(user_id, status);
CREATE INDEX idx_submission_task ON task_submission(task_id, version_no);

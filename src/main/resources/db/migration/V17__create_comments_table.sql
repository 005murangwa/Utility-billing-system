CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    bill_id    BIGINT       NOT NULL,
    comment    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_bill FOREIGN KEY (bill_id) REFERENCES bills (id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_user_id ON comments (user_id);
CREATE INDEX idx_comments_bill_id ON comments (bill_id);
CREATE INDEX idx_comments_created_at ON comments (created_at);

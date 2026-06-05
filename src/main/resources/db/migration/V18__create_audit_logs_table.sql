CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(20)  NOT NULL,
    entity_name  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    timestamp    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_entity_name ON audit_logs (entity_name);
CREATE INDEX idx_audit_logs_entity_id ON audit_logs (entity_id);
CREATE INDEX idx_audit_logs_performed_by ON audit_logs (performed_by);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp);

ALTER TABLE audit_logs
    ADD COLUMN old_value TEXT,
    ADD COLUMN new_value TEXT;

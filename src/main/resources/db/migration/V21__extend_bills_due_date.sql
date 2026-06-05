ALTER TABLE bills
    ADD COLUMN due_date DATE,
    ADD COLUMN late_penalty_applied BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE bills SET late_penalty_applied = FALSE WHERE late_penalty_applied IS NULL;

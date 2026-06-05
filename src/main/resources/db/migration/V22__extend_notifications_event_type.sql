ALTER TABLE notifications
    ADD COLUMN event_type VARCHAR(50),
    ADD COLUMN reference_id BIGINT;

CREATE UNIQUE INDEX uk_notifications_customer_event_reference
    ON notifications (customer_id, event_type, reference_id)
    WHERE event_type IS NOT NULL AND reference_id IS NOT NULL;

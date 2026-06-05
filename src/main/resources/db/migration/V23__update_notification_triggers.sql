DROP TRIGGER IF EXISTS trg_bill_generated ON bills;

CREATE OR REPLACE FUNCTION notify_bill_approved()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name TEXT;
    v_period        TEXT;
    v_message       TEXT;
BEGIN
    IF OLD.approved IS DISTINCT FROM TRUE AND NEW.approved = TRUE THEN
        SELECT full_name
        INTO v_customer_name
        FROM customers
        WHERE id = NEW.customer_id;

        v_period := format_bill_period(NEW.month, NEW.year);

        v_message := 'Dear ' || v_customer_name || ', Your ' || v_period
            || ' utility bill of ' || TRIM(TO_CHAR(NEW.total_amount, '999,999,999.99'))
            || ' FRW has been approved'
            || CASE WHEN NEW.due_date IS NOT NULL
                THEN ' and is due by ' || TO_CHAR(NEW.due_date, 'DD Mon YYYY')
                ELSE ''
            END || '.';

        INSERT INTO notifications (customer_id, message, status, event_type, reference_id, created_at, updated_at)
        SELECT NEW.customer_id, v_message, 'UNREAD', 'BILL_APPROVED', NEW.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM notifications
            WHERE customer_id = NEW.customer_id
              AND event_type = 'BILL_APPROVED'
              AND reference_id = NEW.id
        );
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_bill_approved ON bills;
CREATE TRIGGER trg_bill_approved
    AFTER UPDATE OF approved ON bills
    FOR EACH ROW
    EXECUTE FUNCTION notify_bill_approved();

CREATE OR REPLACE FUNCTION notify_bill_paid()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name TEXT;
    v_period        TEXT;
    v_message       TEXT;
BEGIN
    IF OLD.status IS DISTINCT FROM 'PAID' AND NEW.status = 'PAID' THEN
        SELECT full_name
        INTO v_customer_name
        FROM customers
        WHERE id = NEW.customer_id;

        v_period := format_bill_period(NEW.month, NEW.year);

        v_message := 'Dear ' || v_customer_name || ', Your ' || v_period
            || ' utility bill of ' || TRIM(TO_CHAR(NEW.total_amount, '999,999,999.99'))
            || ' FRW has been successfully processed.';

        INSERT INTO notifications (customer_id, message, status, event_type, reference_id, created_at, updated_at)
        SELECT NEW.customer_id, v_message, 'UNREAD', 'BILL_PAID', NEW.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        WHERE NOT EXISTS (
            SELECT 1 FROM notifications
            WHERE customer_id = NEW.customer_id
              AND event_type = 'BILL_PAID'
              AND reference_id = NEW.id
        );
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION format_bill_period(p_month INT, p_year INT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    month_name TEXT;
BEGIN
    month_name := TRIM(TO_CHAR(TO_DATE(p_year || '-' || LPAD(p_month::TEXT, 2, '0') || '-01', 'YYYY-MM-DD'), 'Month'));
    RETURN month_name || '/' || p_year;
END;
$$;

CREATE OR REPLACE FUNCTION notify_bill_generated()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_customer_name TEXT;
    v_period        TEXT;
    v_message       TEXT;
BEGIN
    SELECT full_name
    INTO v_customer_name
    FROM customers
    WHERE id = NEW.customer_id;

    v_period := format_bill_period(NEW.month, NEW.year);

    v_message := 'Dear ' || v_customer_name || ', Your ' || v_period
        || ' utility bill of ' || TRIM(TO_CHAR(NEW.total_amount, '999,999,999.99')) || ' FRW has been generated.';

    INSERT INTO notifications (customer_id, message, status, created_at, updated_at)
    VALUES (NEW.customer_id, v_message, 'UNREAD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    RETURN NEW;
END;
$$;

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

        INSERT INTO notifications (customer_id, message, status, created_at, updated_at)
        VALUES (NEW.customer_id, v_message, 'UNREAD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_bill_generated ON bills;
CREATE TRIGGER trg_bill_generated
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE FUNCTION notify_bill_generated();

DROP TRIGGER IF EXISTS trg_bill_paid ON bills;
CREATE TRIGGER trg_bill_paid
    AFTER UPDATE OF status ON bills
    FOR EACH ROW
    EXECUTE FUNCTION notify_bill_paid();

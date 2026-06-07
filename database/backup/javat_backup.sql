--
-- PostgreSQL database dump
--

\restrict HmsYCaokvTQsjgx6CA6P8Xo1csLkbhAxONLu3kR944QKFV5BtMqUOjoarFc4qAW

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


--
-- Name: format_bill_period(integer, integer); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.format_bill_period(p_month integer, p_year integer) RETURNS text
    LANGUAGE plpgsql
    AS $$
DECLARE
    month_name TEXT;
BEGIN
    month_name := TRIM(TO_CHAR(TO_DATE(p_year || '-' || LPAD(p_month::TEXT, 2, '0') || '-01', 'YYYY-MM-DD'), 'Month'));
    RETURN month_name || '/' || p_year;
END;
$$;


--
-- Name: notify_bill_approved(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.notify_bill_approved() RETURNS trigger
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


--
-- Name: notify_bill_generated(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.notify_bill_generated() RETURNS trigger
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


--
-- Name: notify_bill_paid(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.notify_bill_paid() RETURNS trigger
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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    action character varying(20) NOT NULL,
    entity_name character varying(50) NOT NULL,
    entity_id bigint NOT NULL,
    performed_by character varying(100) NOT NULL,
    "timestamp" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    old_value text,
    new_value text
);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: bills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bills (
    id bigint NOT NULL,
    bill_reference character varying(50) NOT NULL,
    customer_id bigint NOT NULL,
    meter_id bigint NOT NULL,
    tariff_id bigint NOT NULL,
    meter_reading_id bigint NOT NULL,
    month integer NOT NULL,
    year integer NOT NULL,
    consumption numeric(12,2) NOT NULL,
    amount numeric(14,2) NOT NULL,
    vat_amount numeric(14,2) NOT NULL,
    penalty_amount numeric(14,2) NOT NULL,
    service_charge numeric(14,2) NOT NULL,
    total_amount numeric(14,2) NOT NULL,
    balance numeric(14,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approved boolean DEFAULT false NOT NULL,
    generated_date date NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    due_date date,
    late_penalty_applied boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_bills_amounts CHECK (((consumption >= (0)::numeric) AND (amount >= (0)::numeric) AND (vat_amount >= (0)::numeric) AND (penalty_amount >= (0)::numeric) AND (service_charge >= (0)::numeric) AND (total_amount >= (0)::numeric) AND (balance >= (0)::numeric))),
    CONSTRAINT chk_bills_month CHECK (((month >= 1) AND (month <= 12)))
);


--
-- Name: bills_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bills_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bills_id_seq OWNED BY public.bills.id;


--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    bill_id bigint NOT NULL,
    comment text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: comments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.comments_id_seq OWNED BY public.comments.id;


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id bigint NOT NULL,
    full_name character varying(200) NOT NULL,
    national_id character varying(20) NOT NULL,
    email character varying(100) NOT NULL,
    phone_number character varying(20) NOT NULL,
    address character varying(500) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: jwt_blacklist; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jwt_blacklist (
    id bigint NOT NULL,
    jti character varying(64) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    blacklisted_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: jwt_blacklist_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.jwt_blacklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: jwt_blacklist_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.jwt_blacklist_id_seq OWNED BY public.jwt_blacklist.id;


--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meter_readings (
    id bigint NOT NULL,
    meter_id bigint NOT NULL,
    previous_reading numeric(12,2) NOT NULL,
    current_reading numeric(12,2) NOT NULL,
    reading_date date NOT NULL,
    month integer NOT NULL,
    year integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_meter_readings_month CHECK (((month >= 1) AND (month <= 12))),
    CONSTRAINT chk_meter_readings_values CHECK ((current_reading > previous_reading)),
    CONSTRAINT chk_meter_readings_year CHECK ((year >= 2000))
);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meter_readings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meter_readings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meter_readings_id_seq OWNED BY public.meter_readings.id;


--
-- Name: meters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meters (
    id bigint NOT NULL,
    meter_number character varying(50) NOT NULL,
    meter_type character varying(20) NOT NULL,
    installation_date date NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    customer_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: meters_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.meters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: meters_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.meters_id_seq OWNED BY public.meters.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    message text NOT NULL,
    status character varying(20) DEFAULT 'UNREAD'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    event_type character varying(50),
    reference_id bigint
);


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: otps; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otps (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    code_hash character varying(255) NOT NULL,
    otp_type character varying(50) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: otps_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.otps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: otps_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.otps_id_seq OWNED BY public.otps.id;


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    bill_id bigint NOT NULL,
    amount_paid numeric(14,2) NOT NULL,
    payment_method character varying(20) NOT NULL,
    payment_date date NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_payments_amount CHECK ((amount_paid > (0)::numeric))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- Name: tariffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tariffs (
    id bigint NOT NULL,
    meter_type character varying(20) NOT NULL,
    rate numeric(12,4) NOT NULL,
    service_charge numeric(12,4) NOT NULL,
    vat numeric(5,2) NOT NULL,
    penalty_rate numeric(12,4) NOT NULL,
    version integer NOT NULL,
    effective_date date NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_tariffs_penalty_rate CHECK ((penalty_rate >= (0)::numeric)),
    CONSTRAINT chk_tariffs_rate CHECK ((rate >= (0)::numeric)),
    CONSTRAINT chk_tariffs_service_charge CHECK ((service_charge >= (0)::numeric)),
    CONSTRAINT chk_tariffs_vat CHECK ((vat >= (0)::numeric))
);


--
-- Name: tariffs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tariffs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tariffs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tariffs_id_seq OWNED BY public.tariffs.id;


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: user_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_roles_id_seq OWNED BY public.user_roles.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password character varying(255) NOT NULL,
    first_name character varying(100),
    last_name character varying(100),
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    full_name character varying(200),
    phone_number character varying(20),
    email_verified boolean DEFAULT false NOT NULL,
    first_login boolean DEFAULT false NOT NULL
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: bills id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills ALTER COLUMN id SET DEFAULT nextval('public.bills_id_seq'::regclass);


--
-- Name: comments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments ALTER COLUMN id SET DEFAULT nextval('public.comments_id_seq'::regclass);


--
-- Name: customers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers ALTER COLUMN id SET DEFAULT nextval('public.customers_id_seq'::regclass);


--
-- Name: jwt_blacklist id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_blacklist ALTER COLUMN id SET DEFAULT nextval('public.jwt_blacklist_id_seq'::regclass);


--
-- Name: meter_readings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings ALTER COLUMN id SET DEFAULT nextval('public.meter_readings_id_seq'::regclass);


--
-- Name: meters id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters ALTER COLUMN id SET DEFAULT nextval('public.meters_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: otps id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps ALTER COLUMN id SET DEFAULT nextval('public.otps_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- Name: tariffs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariffs ALTER COLUMN id SET DEFAULT nextval('public.tariffs_id_seq'::regclass);


--
-- Name: user_roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles ALTER COLUMN id SET DEFAULT nextval('public.user_roles_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_logs (id, action, entity_name, entity_id, performed_by, "timestamp", old_value, new_value) FROM stdin;
1	CREATE	Customer	1	brillanteigabemurangwa	2026-06-05 11:15:16.165291	\N	\N
2	CREATE	Tariff	1	brillanteigabemurangwa	2026-06-05 11:15:47.734492	\N	\N
3	CREATE	Meter	1	brillanteigabemurangwa	2026-06-05 11:16:20.288225	\N	\N
4	CREATE	Tariff	2	brillanteigabemurangwa	2026-06-05 12:29:17.74405	\N	\N
5	CREATE	User	10	brillanteigabemurangwa	2026-06-05 12:32:43.292681	\N	{"roles":"ROLE_OPERATOR"}
6	CREATE	MeterReading	1	murangwabr	2026-06-05 12:42:02.78415	\N	\N
7	CREATE	Bill	1	brillanteigabemurangwa	2026-06-05 12:43:44.435478	\N	\N
8	APPROVE	Bill	1	brillanteigabemurangwa	2026-06-05 12:44:04.230662	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
9	CREATE	User	11	brillanteigabemurangwa	2026-06-05 12:45:34.428998	\N	{"roles":"ROLE_FINANCE"}
10	CREATE	Payment	1	migzgloire	2026-06-05 12:48:47.112127	\N	\N
11	CREATE	Customer	2	brillanteigabemurangwa	2026-06-05 13:15:17.78032	\N	\N
12	CREATE	Customer	3	brillanteigabemurangwa	2026-06-05 13:23:27.120135	\N	\N
13	CREATE	Customer	4	brillanteigabemurangwa	2026-06-05 13:24:14.209481	\N	\N
14	CREATE	Meter	2	brillanteigabemurangwa	2026-06-05 13:27:58.582671	\N	\N
15	CREATE	MeterReading	2	murangwabr	2026-06-05 13:27:58.759668	\N	\N
16	CREATE	Bill	2	brillanteigabemurangwa	2026-06-05 13:27:58.81567	\N	\N
17	APPROVE	Bill	2	brillanteigabemurangwa	2026-06-05 13:27:58.865664	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
18	CREATE	Customer	5	brillanteigabemurangwa	2026-06-05 13:33:00.102062	\N	\N
19	CREATE	Customer	6	brillanteigabemurangwa	2026-06-05 13:33:12.162872	\N	\N
20	CREATE	User	16	brillanteigabemurangwa	2026-06-05 13:33:16.331364	\N	{"roles":"ROLE_CUSTOMER"}
21	CREATE	Payment	2	migzgloire	2026-06-05 13:38:19.392735	\N	\N
22	CREATE	Payment	3	migzgloire	2026-06-05 13:43:41.419354	\N	\N
23	CREATE	MeterReading	3	murangwabr	2026-06-05 13:54:08.457102	\N	\N
24	CREATE	Bill	3	brillanteigabemurangwa	2026-06-05 13:55:20.270745	\N	\N
25	APPROVE	Bill	3	brillanteigabemurangwa	2026-06-05 13:55:38.239493	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
26	CREATE	Payment	4	migzgloire	2026-06-05 13:57:19.213173	\N	\N
27	CREATE	MeterReading	4	murangwabr	2026-06-05 14:11:03.279753	\N	\N
28	CREATE	Bill	4	brillanteigabemurangwa	2026-06-05 14:12:41.417687	\N	\N
29	APPROVE	Bill	4	brillanteigabemurangwa	2026-06-05 14:14:01.64412	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
30	CREATE	Payment	5	migzgloire	2026-06-05 14:16:08.05849	\N	\N
31	CREATE	Customer	7	SYSTEM	2026-06-07 14:27:39.513262	\N	\N
32	CREATE	Customer	8	SYSTEM	2026-06-07 14:30:19.420644	\N	\N
33	CREATE	Tariff	3	brillanteigabemurangwa	2026-06-07 14:34:34.393734	\N	\N
34	CREATE	Meter	3	brillanteigabemurangwa	2026-06-07 14:35:12.552843	\N	\N
35	CREATE	MeterReading	5	murangwabr	2026-06-07 14:36:54.994828	\N	\N
36	CREATE	Bill	5	brillanteigabemurangwa	2026-06-07 14:41:46.566171	\N	\N
37	APPROVE	Bill	5	brillanteigabemurangwa	2026-06-07 14:41:53.609121	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
38	CREATE	Payment	6	migzgloire	2026-06-07 14:42:55.940764	\N	\N
39	CREATE	Meter	4	brillanteigabemurangwa	2026-06-07 14:57:05.466386	\N	\N
40	CREATE	MeterReading	6	murangwabr	2026-06-07 14:58:02.833724	\N	\N
41	CREATE	Bill	6	brillanteigabemurangwa	2026-06-07 14:58:52.020655	\N	\N
42	APPROVE	Bill	6	brillanteigabemurangwa	2026-06-07 14:59:00.864683	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
43	CREATE	Payment	7	migzgloire	2026-06-07 15:00:26.831756	\N	\N
44	CREATE	Payment	8	migzgloire	2026-06-07 15:03:11.863583	\N	\N
45	CREATE	MeterReading	7	murangwabr	2026-06-07 15:07:16.163809	\N	\N
46	CREATE	Bill	7	brillanteigabemurangwa	2026-06-07 15:10:54.94661	\N	\N
47	APPROVE	Bill	7	brillanteigabemurangwa	2026-06-07 15:11:02.488129	{"approved":"false"}	{"approved":"true","status":"APPROVED"}
48	CREATE	Payment	9	migzgloire	2026-06-07 15:11:59.889254	\N	\N
\.


--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bills (id, bill_reference, customer_id, meter_id, tariff_id, meter_reading_id, month, year, consumption, amount, vat_amount, penalty_amount, service_charge, total_amount, balance, status, approved, generated_date, created_at, updated_at, due_date, late_penalty_applied) FROM stdin;
2	BILL-202606-M00002-0002	4	2	2	2	6	2026	45.00	20250.00	3915.00	2250.00	1500.00	27915.00	0.00	PAID	t	2026-06-05	2026-06-05 13:27:58.810666	2026-06-05 13:38:19.396735	2026-07-05	f
1	BILL-202606-M00001-0001	1	1	2	1	6	2026	25.50	11475.00	2335.50	1275.00	1500.00	16585.50	0.00	PAID	t	2026-06-05	2026-06-05 12:43:44.430478	2026-06-05 13:43:41.462174	2026-07-05	f
3	BILL-202605-M00002-0001	4	2	2	3	5	2026	45.00	20250.00	3915.00	2250.00	1500.00	27915.00	0.00	PAID	t	2026-06-05	2026-06-05 13:55:20.26771	2026-06-05 13:57:19.225759	2026-07-05	f
4	BILL-202604-M00002-0001	4	2	2	4	4	2026	45.00	20250.00	3915.00	2250.00	1500.00	27915.00	0.00	PAID	t	2026-06-05	2026-06-05 14:12:41.415689	2026-06-05 14:16:08.101795	2026-07-05	f
5	BILL-202606-M00003-0003	6	3	3	5	6	2026	55.75	27875.00	5377.50	278.75	2000.00	35531.25	7616.25	PARTIALLY_PAID	t	2026-06-07	2026-06-07 14:41:46.563138	2026-06-07 14:42:55.944763	2026-07-07	f
6	BILL-202606-M00004-0004	8	4	3	6	6	2026	50.00	25000.00	4860.00	250.00	2000.00	32110.00	0.00	PAID	t	2026-06-07	2026-06-07 14:58:52.018657	2026-06-07 15:03:11.86758	2026-07-07	f
7	BILL-202605-M00004-0002	8	4	3	7	5	2026	50.00	25000.00	4860.00	250.00	2000.00	32110.00	0.00	PAID	t	2026-06-07	2026-06-07 15:10:54.943615	2026-06-07 15:11:59.933128	2026-07-07	f
\.


--
-- Data for Name: comments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.comments (id, user_id, bill_id, comment, created_at) FROM stdin;
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (id, full_name, national_id, email, phone_number, address, status, created_at, updated_at) FROM stdin;
1	Jean Baptiste Uwimana	1199887766554433	jean.uwimana@example.com	+250788123456	Kigali, Gasabo, Remera	ACTIVE	2026-06-05 11:15:16.159292	2026-06-05 11:15:16.159292
2	Uwase kevine	1199887766554434	uwasek21@gmail.com	+250788999888	Kigali, Rwanda	ACTIVE	2026-06-05 13:15:17.770361	2026-06-05 13:15:17.770361
3	aurore mukundwa	1234567890123456	mukundwaa35@gmail.comm	+250788923456	Kigali, Rwanda	INACTIVE	2026-06-05 13:23:27.117138	2026-06-05 13:23:27.117138
4	aurore mukundwa	1236567890123456	mukundwaa35@gmail.com	+250788920456	Kigali, Rwanda	ACTIVE	2026-06-05 13:24:14.207481	2026-06-05 13:25:02.56318
6	Hellooooooo	9876543210987654	whenever67e@gmail.com	+250788119922	Kigali, Rwanda	ACTIVE	2026-06-05 13:33:12.160872	2026-06-05 13:33:12.160872
7	Awet Feseha	1198765432109876	awet.feseha1@gmail.com	+250787654321	Kigali, Rwanda	INACTIVE	2026-06-07 14:27:39.507262	2026-06-07 14:27:39.507262
8	Awet Feseha	1198765932109876	awet.fesseha1@gmail.com	+250787694321	Kigali, Rwanda	ACTIVE	2026-06-07 14:30:19.419596	2026-06-07 14:30:58.261254
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create roles table	SQL	V1__create_roles_table.sql	-332695245	postgres	2026-06-05 10:44:16.486284	17	t
2	2	create users table	SQL	V2__create_users_table.sql	-1428743163	postgres	2026-06-05 10:44:16.524371	14	t
3	3	create user roles table	SQL	V3__create_user_roles_table.sql	-2142742733	postgres	2026-06-05 10:44:16.553518	16	t
4	4	extend users for verification	SQL	V4__extend_users_for_verification.sql	1066127910	postgres	2026-06-05 10:44:16.579319	9	t
5	5	create otps table	SQL	V5__create_otps_table.sql	-679217079	postgres	2026-06-05 10:44:16.598685	9	t
6	6	create refresh tokens table	SQL	V6__create_refresh_tokens_table.sql	-1844221375	postgres	2026-06-05 10:44:16.6156	10	t
7	7	create jwt blacklist table	SQL	V7__create_jwt_blacklist_table.sql	-1639125487	postgres	2026-06-05 10:44:16.633423	10	t
8	8	create password reset tokens table	SQL	V8__create_password_reset_tokens_table.sql	68747129	postgres	2026-06-05 10:44:16.651366	10	t
9	9	create customers table	SQL	V9__create_customers_table.sql	-1832874919	postgres	2026-06-05 10:44:16.669208	18	t
10	10	create meters table	SQL	V10__create_meters_table.sql	1900406618	postgres	2026-06-05 10:44:16.698078	55	t
11	11	create meter readings table	SQL	V11__create_meter_readings_table.sql	468656522	postgres	2026-06-05 10:44:16.763582	19	t
12	12	create tariffs table	SQL	V12__create_tariffs_table.sql	-2023007258	postgres	2026-06-05 10:44:16.794041	18	t
13	13	create bills table	SQL	V13__create_bills_table.sql	672879905	postgres	2026-06-05 10:44:16.823353	29	t
14	14	create payments table	SQL	V14__create_payments_table.sql	-1145505522	postgres	2026-06-05 10:44:16.866311	15	t
15	15	create notifications table	SQL	V15__create_notifications_table.sql	787029445	postgres	2026-06-05 10:44:16.892602	19	t
16	16	create notification triggers	SQL	V16__create_notification_triggers.sql	-569812066	postgres	2026-06-05 10:44:16.923527	12	t
17	17	create comments table	SQL	V17__create_comments_table.sql	593724578	postgres	2026-06-05 10:44:16.949107	18	t
18	18	create audit logs table	SQL	V18__create_audit_logs_table.sql	335277209	postgres	2026-06-05 10:44:16.979272	18	t
19	19	extend users first login	SQL	V19__extend_users_first_login.sql	832481227	postgres	2026-06-05 10:44:17.008705	4	t
20	20	extend audit logs values	SQL	V20__extend_audit_logs_values.sql	1406716509	postgres	2026-06-05 10:44:17.02116	4	t
21	21	extend bills due date	SQL	V21__extend_bills_due_date.sql	-1391236601	postgres	2026-06-05 10:44:17.033216	11	t
22	22	extend notifications event type	SQL	V22__extend_notifications_event_type.sql	204501058	postgres	2026-06-05 10:44:17.051075	4	t
23	23	update notification triggers	SQL	V23__update_notification_triggers.sql	-1965776388	postgres	2026-06-05 10:44:17.064842	5	t
24	24	ensure missing auth tables	SQL	V24__ensure_missing_auth_tables.sql	948779313	postgres	2026-06-05 10:44:17.0783	12	t
25	25	repair password reset tokens schema	SQL	V25__repair_password_reset_tokens_schema.sql	2118798856	postgres	2026-06-05 10:44:17.102953	29	t
\.


--
-- Data for Name: jwt_blacklist; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.jwt_blacklist (id, jti, expires_at, blacklisted_at) FROM stdin;
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meter_readings (id, meter_id, previous_reading, current_reading, reading_date, month, year, created_at, updated_at) FROM stdin;
1	1	100.00	125.50	2026-06-05	6	2026	2026-06-05 12:42:02.77915	2026-06-05 12:42:02.77915
2	2	100.00	145.00	2026-06-01	6	2026	2026-06-05 13:27:58.75567	2026-06-05 13:27:58.75567
3	2	55.00	100.00	2026-05-31	5	2026	2026-06-05 13:54:08.4521	2026-06-05 13:54:08.4521
4	2	10.00	55.00	2026-04-30	4	2026	2026-06-05 14:11:03.277759	2026-06-05 14:11:03.277759
5	3	100.00	155.75	2026-06-05	6	2026	2026-06-07 14:36:54.990791	2026-06-07 14:36:54.990791
6	4	100.00	150.00	2026-06-07	6	2026	2026-06-07 14:58:02.829724	2026-06-07 14:58:02.829724
7	4	100.00	150.00	2026-05-07	5	2026	2026-06-07 15:07:16.161815	2026-06-07 15:07:16.161815
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meters (id, meter_number, meter_type, installation_date, status, customer_id, created_at, updated_at) FROM stdin;
1	WM-001-KGL	WATER	2026-01-15	ACTIVE	1	2026-06-05 11:16:20.284221	2026-06-05 11:16:20.284221
2	WM-MUKU-001	WATER	2026-01-15	ACTIVE	4	2026-06-05 13:27:58.577666	2026-06-05 13:27:58.577666
3	WM-AWET-001	WATER	2026-01-01	ACTIVE	6	2026-06-07 14:35:12.509391	2026-06-07 14:35:12.509391
4	WM-AWET-FES-001	WATER	2026-01-01	ACTIVE	8	2026-06-07 14:57:05.463377	2026-06-07 14:57:05.463377
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, customer_id, message, status, created_at, updated_at, event_type, reference_id) FROM stdin;
1	1	Dear Jean Baptiste Uwimana, your 6/2026 utility bill BILL-202606-M00001-0001 has been generated and is pending approval.	UNREAD	2026-06-05 12:43:44.452475	2026-06-05 12:43:44.452475	BILL_GENERATED	1
2	1	Dear Jean Baptiste Uwimana, Your June/2026 utility bill of 16,585.50 FRW has been approved and is due by 05 Jul 2026.	UNREAD	2026-06-05 12:44:04.222913	2026-06-05 12:44:04.222913	BILL_APPROVED	1
3	4	Dear aurore mukundwa, your 6/2026 utility bill BILL-202606-M00002-0002 has been generated and is pending approval.	UNREAD	2026-06-05 13:27:58.828667	2026-06-05 13:27:58.828667	BILL_GENERATED	2
4	4	Dear aurore mukundwa, Your June/2026 utility bill of 27,915.00 FRW has been approved and is due by 05 Jul 2026.	UNREAD	2026-06-05 13:27:58.858619	2026-06-05 13:27:58.858619	BILL_APPROVED	2
5	4	Dear aurore mukundwa, Your June/2026 utility bill of 27,915.00 FRW has been successfully processed.	UNREAD	2026-06-05 13:38:19.382155	2026-06-05 13:38:19.382155	BILL_PAID	2
6	1	Dear Jean Baptiste Uwimana, Your June/2026 utility bill of 16,585.50 FRW has been successfully processed.	UNREAD	2026-06-05 13:43:41.412822	2026-06-05 13:43:41.412822	BILL_PAID	1
7	4	Dear aurore mukundwa, your 5/2026 utility bill BILL-202605-M00002-0001 has been generated and is pending approval.	UNREAD	2026-06-05 13:55:20.279705	2026-06-05 13:55:20.279705	BILL_GENERATED	3
8	4	Dear aurore mukundwa, Your May/2026 utility bill of 27,915.00 FRW has been approved and is due by 05 Jul 2026.	UNREAD	2026-06-05 13:55:38.234892	2026-06-05 13:55:38.234892	BILL_APPROVED	3
9	4	Dear aurore mukundwa, Your May/2026 utility bill of 27,915.00 FRW has been successfully processed.	UNREAD	2026-06-05 13:57:19.208379	2026-06-05 13:57:19.208379	BILL_PAID	3
10	4	Dear aurore mukundwa, your 4/2026 utility bill BILL-202604-M00002-0001 has been generated and is pending approval.	UNREAD	2026-06-05 14:12:41.460829	2026-06-05 14:12:41.460829	BILL_GENERATED	4
11	4	Dear aurore mukundwa, Your April/2026 utility bill of 27,915.00 FRW has been approved and is due by 05 Jul 2026.	UNREAD	2026-06-05 14:14:01.641701	2026-06-05 14:14:01.641701	BILL_APPROVED	4
12	4	Dear aurore mukundwa, Your April/2026 utility bill of 27,915.00 FRW has been successfully processed.	UNREAD	2026-06-05 14:16:08.055679	2026-06-05 14:16:08.055679	BILL_PAID	4
13	6	Dear Hellooooooo, your 6/2026 utility bill BILL-202606-M00003-0003 has been generated and is pending approval.	UNREAD	2026-06-07 14:41:46.573278	2026-06-07 14:41:46.573278	BILL_GENERATED	5
14	6	Dear Hellooooooo, Your June/2026 utility bill of 35,531.25 FRW has been approved and is due by 07 Jul 2026.	UNREAD	2026-06-07 14:41:53.602156	2026-06-07 14:41:53.602156	BILL_APPROVED	5
15	8	Dear Awet Feseha, your 6/2026 utility bill BILL-202606-M00004-0004 has been generated and is pending approval.	UNREAD	2026-06-07 14:58:52.107764	2026-06-07 14:58:52.107764	BILL_GENERATED	6
16	8	Dear Awet Feseha, Your June/2026 utility bill of 32,110.00 FRW has been approved and is due by 07 Jul 2026.	UNREAD	2026-06-07 14:59:00.860644	2026-06-07 14:59:00.860644	BILL_APPROVED	6
17	8	Dear Awet Feseha, Your June/2026 utility bill of 32,110.00 FRW has been successfully processed.	UNREAD	2026-06-07 15:03:11.85991	2026-06-07 15:03:11.85991	BILL_PAID	6
18	8	Dear Awet Feseha, your 5/2026 utility bill BILL-202605-M00004-0002 has been generated and is pending approval.	UNREAD	2026-06-07 15:10:54.990158	2026-06-07 15:10:54.990158	BILL_GENERATED	7
19	8	Dear Awet Feseha, Your May/2026 utility bill of 32,110.00 FRW has been approved and is due by 07 Jul 2026.	UNREAD	2026-06-07 15:11:02.483643	2026-06-07 15:11:02.483643	BILL_APPROVED	7
20	8	Dear Awet Feseha, Your May/2026 utility bill of 32,110.00 FRW has been successfully processed.	UNREAD	2026-06-07 15:11:59.885792	2026-06-07 15:11:59.885792	BILL_PAID	7
\.


--
-- Data for Name: otps; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.otps (id, user_id, code_hash, otp_type, expires_at, used, created_at) FROM stdin;
4	9	$2a$10$UWCUBkMD7ueuCsq/1AeahOcxwWSkiUdUicvFMO7q3h7ACHlYL4lka	EMAIL_VERIFICATION	2026-06-05 12:18:52.170632	t	2026-06-05 12:08:52.170632
5	13	$2a$10$cPM5ofmAluTOVxVQM7Mcgu7lVOJvGBIb72fubHYQNj1LCXjJBJl2u	EMAIL_VERIFICATION	2026-06-05 13:33:27.256485	f	2026-06-05 13:23:27.257022
6	15	$2a$10$nddISY4mVrIsIRmoCV5jxOljKgi.ywUwNnvOafrnlCmf62nmFQJx2	EMAIL_VERIFICATION	2026-06-05 13:34:14.297516	t	2026-06-05 13:24:14.29848
7	17	$2a$10$46QLJ8ZXeKFq2d7YsyyoG.dSgXs30o9MnFiS6y7a0Xa.aMvgtj4FW	EMAIL_VERIFICATION	2026-06-07 14:37:39.591261	f	2026-06-07 14:27:39.592407
8	19	$2a$10$09IlWd477a9qHFNpXIwon.GZIu1Uz9LV3cDHEkH5wg6.3FOCmr1QS	EMAIL_VERIFICATION	2026-06-07 14:40:19.522273	t	2026-06-07 14:30:19.522273
\.


--
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.password_reset_tokens (id, user_id, token_hash, expires_at, used, created_at) FROM stdin;
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (id, bill_id, amount_paid, payment_method, payment_date, created_at, updated_at) FROM stdin;
1	1	5000.00	MOMO	2026-06-05	2026-06-05 12:48:47.105127	2026-06-05 12:48:47.105127
2	2	27915.00	MOMO	2026-06-05	2026-06-05 13:38:19.385731	2026-06-05 13:38:19.385731
3	1	11585.50	MOMO	2026-06-05	2026-06-05 13:43:41.415359	2026-06-05 13:43:41.415359
4	3	27915.00	MOMO	2026-06-05	2026-06-05 13:57:19.20925	2026-06-05 13:57:19.20925
5	4	27915.00	MOMO	2026-06-05	2026-06-05 14:16:08.056489	2026-06-05 14:16:08.056489
6	5	27915.00	MOMO	2026-06-05	2026-06-07 14:42:55.890929	2026-06-07 14:42:55.890929
7	6	24000.00	MOMO	2026-06-05	2026-06-07 15:00:26.829752	2026-06-07 15:00:26.829752
8	6	8110.00	MOMO	2026-06-07	2026-06-07 15:03:11.861625	2026-06-07 15:03:11.861625
9	7	32110.00	MOMO	2026-06-07	2026-06-07 15:11:59.887246	2026-06-07 15:11:59.887246
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (id, user_id, token_hash, expires_at, revoked, created_at) FROM stdin;
1	1	5b728ebda95aea7eecba6c7b71b980f61ef86517d5411c6d72df4c25b623794e	2026-06-12 11:11:39.211934	f	2026-06-05 11:11:39.223931
2	1	454af1815fb5aaa306c3bbb32e17920652004eb36b4f6097e1ee29534da207c0	2026-06-12 11:12:01.712981	f	2026-06-05 11:12:01.712981
3	9	d215c88724407a4b604b61725746e4a30b69fbf239438146454c82ecc261ed48	2026-06-12 12:13:54.927219	f	2026-06-05 12:13:54.927219
4	9	352a2bef101be086b62fc63d6047be6dddbf5f125e9f4151e2af4974ba2f4f01	2026-06-12 12:22:36.443396	f	2026-06-05 12:22:36.443396
5	1	cabbf69c1ee3a8643df8abe73bf443e47d503bee23eab027d7baedf9c8cd26b9	2026-06-12 12:27:59.244601	f	2026-06-05 12:27:59.244601
6	10	266db2ee97aa677a55de4babe2250c8d800d5ca99ce328ee555fb0aa59f336d5	2026-06-12 12:35:20.4303	f	2026-06-05 12:35:20.4303
7	10	a0678750aba2a2942a9725cff471f8a55af0878d791166e1b6ea49d721086993	2026-06-12 12:38:02.446058	f	2026-06-05 12:38:02.447056
8	10	a5fc0fb3d1ac9308872dd107060d2857d81dea396ce701223fd1ab1219c0b28e	2026-06-12 12:40:17.919097	f	2026-06-05 12:40:17.920096
9	10	bbe13798de0e3cf513e15aad9ec1adb31457a94ee01c105108de599fa7d18f8d	2026-06-12 12:40:41.370269	f	2026-06-05 12:40:41.371269
10	1	3e61915aa122082607b9e1fb00755b84b429829733e77c7d6f1036a148bdc84d	2026-06-12 12:43:12.641114	f	2026-06-05 12:43:12.641114
11	11	a429d5c98de2521305b30f841ffedd72436d71f93ca33f586a96e503f6a27bcf	2026-06-12 12:46:42.191179	f	2026-06-05 12:46:42.191179
12	11	35f1b28ba4830b75eb91c204f825ed59a9047a52e55333c410a4ec35b9c58927	2026-06-12 12:47:21.953688	f	2026-06-05 12:47:21.95469
13	11	38ba2c625f8adfc7fc46d3b8c2c5dec6b9f4399f6e8992f3d8e61b838d1436a0	2026-06-12 12:47:53.959335	f	2026-06-05 12:47:53.959335
14	9	979740a32c91d6c4f4e1819bec021fd9a007d515ad4c4c71c14fdb1e08bf2adf	2026-06-12 12:49:19.109496	f	2026-06-05 12:49:19.109496
15	1	42ac36bb564d9d05f08fff0e34d6667130fc401543d3e91fd4099c5d94ba76fd	2026-06-12 12:53:27.080806	f	2026-06-05 12:53:27.080806
16	1	f297d0de51706f5e10e9867623648fd2a25bdd9604ad9241a69ebf151dad42c1	2026-06-12 13:10:55.548232	f	2026-06-05 13:10:55.560213
17	1	c824e1400f9b39bd2f914331a2faec4036b4fbdcb6a7dfb41e2bf117dc34348c	2026-06-12 13:14:59.278485	f	2026-06-05 13:14:59.278485
18	1	231e2f58841a7764c5cfc82bfd1ca8485b565df5bf4a74fd8dead2f6302d6bbc	2026-06-12 13:15:04.330755	f	2026-06-05 13:15:04.330755
19	1	30ce8de4eff1d6728afe2dd20fb95904b8626033adc5c91df152aed3a0c0ff8b	2026-06-12 13:15:09.172813	f	2026-06-05 13:15:09.172813
20	1	63e6eae29f6b45d503527bb9cc6990cba61a6d9e597c1e7951b8e6d850ceeba2	2026-06-12 13:15:17.685319	f	2026-06-05 13:15:17.685319
21	9	59f1b1e71f1c23d050c3a2a0e7aab842252cfbc633d2aff3e2fe9c3fa334347c	2026-06-12 13:15:22.468512	f	2026-06-05 13:15:22.468512
22	1	7d770e52b1a1e230291390c63a07739c65fd4212bdc8563a1a697fd392801681	2026-06-12 13:18:00.428312	f	2026-06-05 13:18:00.429314
23	15	ab4a43600175fa04700386a6d6f388a048b2b808b455b80f7ff188b2cd4e4f6d	2026-06-12 13:26:06.192089	f	2026-06-05 13:26:06.192089
24	1	e42a9a0751cb05289ab0a312993184cf6c07775d6a75842e9dec937043dde2b6	2026-06-12 13:27:48.399885	f	2026-06-05 13:27:48.399885
25	1	e6e81180964b0ad7c2d33ff024cf1024d99afd551107086cbe321e5a5f832907	2026-06-12 13:27:58.331233	f	2026-06-05 13:27:58.331233
26	15	fb6281587b6d3bc69f3f756eccb7fd76c2b5bb3ac89fe58ab01512957836e183	2026-06-12 13:27:58.461981	f	2026-06-05 13:27:58.461981
27	10	d3f895d7470665ecc96e49c8ad4ed8d924d3bfffb514bf9b0a44e07909ba4f2e	2026-06-12 13:27:58.714666	f	2026-06-05 13:27:58.714666
28	1	829db76b8e92d58ab50a848b133ce8bed751a5127238bc4ebe42756bd75bd761	2026-06-12 13:30:36.632372	f	2026-06-05 13:30:36.632372
29	16	4d73317a542faf918a0a66c47a0aef899127c6206dd6a8b9d670603fbe29ffb0	2026-06-12 13:35:28.578051	f	2026-06-05 13:35:28.578051
30	16	55a7cb0b9fb98cec731c41b0e003b7fbd8729215dc843fa1f3c2a678f0715c2a	2026-06-12 13:36:34.310741	f	2026-06-05 13:36:34.310741
31	16	9130be4233a4250a746512cc958175b817ed307ed5ff983e883a0d49129c33b0	2026-06-12 13:36:52.894753	f	2026-06-05 13:36:52.894753
32	1	129c53f0c26f50128f37a84d9a27b073a94c849a7747f0d8d59585cf03953cab	2026-06-12 13:38:11.234361	f	2026-06-05 13:38:11.235364
33	11	56a00af65022daa6c93f130f1cc3e451ac5ecf02c576fca33513de35ae640ea3	2026-06-12 13:38:11.366364	f	2026-06-05 13:38:11.366364
34	15	45fe2ce0f5bc62a0e17a2f8cff53c3e5ec4bf7d05f94d347ebac415f735cf496	2026-06-12 13:38:11.501362	f	2026-06-05 13:38:11.501362
35	11	2fef1a11baf1aef4dde449a0f1eb0890a8a1ccc1fd813cd2262f2cf79c1aca86	2026-06-12 13:38:19.36473	f	2026-06-05 13:38:19.36473
36	11	c89042e8b9d88a23d5707d3a9264e25b8c78bb1e1f7094900d5738056b3a34b2	2026-06-12 13:41:28.729923	f	2026-06-05 13:41:28.730926
37	1	9087381ae90d690a56db121550710a01e297847c3c6c060437dee13332569edc	2026-06-12 13:43:06.084255	f	2026-06-05 13:43:06.084255
38	1	ecad6d638112284b54d9242b37af1b9db281a176a386bfb36c6e34ed887ac4d6	2026-06-12 13:44:42.966542	f	2026-06-05 13:44:42.966542
39	1	52fb4737c30e61e7d5a1126891b2698f64f55403d66fa345b587a9b0ca9d2d41	2026-06-12 13:44:54.890682	f	2026-06-05 13:44:54.890682
40	1	f2625cc1883602f1b7fe8c37271f3034f110f5aee102a2e8a9d7b0072682e437	2026-06-12 13:45:08.655972	f	2026-06-05 13:45:08.655972
41	10	02d2c5f3122fada180dcdf29672e661e272afd3c72a67ec7b7bed61c2514269e	2026-06-12 13:46:23.50615	f	2026-06-05 13:46:23.50615
42	1	a2ebf09631281374bc765b496be4dcd7109eb05c08ac9f77bec11f6d7535fc7f	2026-06-12 13:52:50.139023	f	2026-06-05 13:52:50.139023
43	10	935466e15143eb670ada63d15ab2e58c92cd25c6d8c78e3f86715e14c425dfb0	2026-06-12 13:52:50.322022	f	2026-06-05 13:52:50.322022
44	10	5c126a44d43fba5e66d594d036725e53299bb138744debb2c79499b7f869719b	2026-06-12 13:52:50.460066	f	2026-06-05 13:52:50.460066
45	10	1e6896b4f05b3974a0d08d64d0a85881acdb83a96a001686bd80d08aa57223f2	2026-06-12 13:52:50.600038	f	2026-06-05 13:52:50.600038
46	10	f6bd23cba55ffae43d67ca2afd9b21daf3e8b2d73312ecb5a9da8c4c5635da46	2026-06-12 13:52:50.732022	f	2026-06-05 13:52:50.733022
47	10	7a07ecc48aa165e937bfefca414a688a689d0b21499b0ef25710da8deb209fcc	2026-06-12 13:52:58.283352	f	2026-06-05 13:52:58.283352
48	1	c5c53d1e32ffbae82a6fbe414febe396bf8813f164f51d1b0521f9f2de90c021	2026-06-12 13:54:45.569918	f	2026-06-05 13:54:45.569918
49	11	715302386363237fd8cffcc2897274588c13a6526be6f05d1a56439c128290e7	2026-06-12 13:56:36.380398	f	2026-06-05 13:56:36.380398
50	15	e989b4531874fb71315f0dd953f2889ebce959932009a30f0db9c369e5e0fb26	2026-06-12 13:57:46.580833	f	2026-06-05 13:57:46.580833
51	1	3587be1421db6e8a3c7c1d2f51bdac3e2db1c6c2060a432dcd2fe48f737cd0ff	2026-06-12 14:04:03.150014	f	2026-06-05 14:04:03.150014
52	10	b61c7edc1841b9d92c9a29760678012b8df313c633e8195cf80da03d850240a8	2026-06-12 14:05:03.133653	f	2026-06-05 14:05:03.133653
53	1	151a4d17a4b7b9bf35c13719bebd8ec8a64b4f531607913dc693a143ccf6bd4e	2026-06-12 14:07:16.937532	f	2026-06-05 14:07:16.937532
54	1	172c46a0b4b5ae83f0eb1740e7030b785e208149a45bb10d4742ace88d2a12e9	2026-06-12 14:08:25.663175	f	2026-06-05 14:08:25.66418
55	10	036bcae95cc363a5eb7a6ec2f89a11ef419fa734e1ff371c567c6e00bda85e64	2026-06-12 14:08:25.93705	f	2026-06-05 14:08:25.938053
56	10	5a6ef84da34a0f0d4f2b784f086888ff73853bf3b74e2c461ebe443a0b020747	2026-06-12 14:09:51.975462	f	2026-06-05 14:09:51.976462
57	1	dcdac28151a55fae15499a8104fd2ac1d7d608190632cf588e44039b1fcfdd97	2026-06-12 14:12:04.83216	f	2026-06-05 14:12:04.83216
58	11	97e01b444f9dc79ee4367c45ddee0f624594d6b63709ee0ceb25d79bfe561719	2026-06-12 14:14:36.399041	f	2026-06-05 14:14:36.399041
59	1	766bf19ea696f0f89102e63c336e7b8488955f4199c871c90c3e8ec2e0dc3842	2026-06-12 14:15:31.755002	f	2026-06-05 14:15:31.756002
60	10	5f955e6f8a945f070e6f21bcd07cf2ccd8c8f3e31104865f25d1b6f501cd7a5e	2026-06-12 14:15:32.020001	f	2026-06-05 14:15:32.021001
61	15	d41c54e1b3e7e261280c552dd924f53bc6c9f84ea8810c97da5afbe42e10724c	2026-06-12 14:16:36.040518	f	2026-06-05 14:16:36.040518
62	15	5d61138437c8cbc44346264f04a7d3eab55ea3ca9e94c48d00104f55f77c528d	2026-06-12 14:23:18.103553	f	2026-06-05 14:23:18.103553
63	15	ee4244d63ce35cf44b8d7516d98954f95e4400fa3b1899d2673288c52f30ef21	2026-06-12 14:25:29.197873	f	2026-06-05 14:25:29.197873
64	11	8c69732d836f6a72949fe923c2de0d2b5bf96813bb399320bcabe0d84906c960	2026-06-12 14:25:33.720368	f	2026-06-05 14:25:33.720368
65	15	f35a8509708174090388295b7b1973bd7798e9a8a08848fe313d281c5a079997	2026-06-12 14:25:33.882725	f	2026-06-05 14:25:33.883721
66	15	afd235f0a8a87e1067e0962092e3175c6d16afa92e4ae718e5514552b1b780a6	2026-06-12 14:25:34.011723	f	2026-06-05 14:25:34.011723
67	15	d02eb2bfa3230100c2c20376ee2ac51673677fac6eec99f9e7450717ac9f8cbe	2026-06-12 14:25:34.138719	f	2026-06-05 14:25:34.139722
68	15	7b3f50fae4f71aea00558d9fca81c3df7676386fc232d09e3e1d250f1a6ce7c9	2026-06-12 14:26:37.747068	f	2026-06-05 14:26:37.756062
69	11	7e294dad1efc637edb6d0ea31f6db85f5ab44c006f2c0e73f642a38c3bdcdb9b	2026-06-12 14:26:37.988234	f	2026-06-05 14:26:37.988234
70	15	d56dda61a5032c609a21fa621e4a18c6ab2bd50335f92254d326014bf023bb1f	2026-06-12 14:31:07.427739	f	2026-06-05 14:31:07.427739
71	19	d43179e261f35781fe0bb4fa4f0d9b9894e5499e0aa330f0141aafb1ef374fde	2026-06-14 14:32:47.809763	f	2026-06-07 14:32:47.809763
72	1	f8b8ab474fea825827875eb838a43c5308c746182e91d58579356f95831c02d5	2026-06-14 14:33:14.480112	f	2026-06-07 14:33:14.481111
73	10	d5e03101be5d9506549f4a492543e3af015c2177ccb49f60590e2ccf79fd0cdf	2026-06-14 14:35:29.143516	f	2026-06-07 14:35:29.143516
74	1	5de392917d468efc741b1679004237c40cef906df6ec79e09f259ae0879d587b	2026-06-14 14:37:26.958085	f	2026-06-07 14:37:26.958085
75	11	ea1eac3d7a25294888e1e66e3f1884c65dd5f9889d35ae39b7a5136d51ae229b	2026-06-14 14:38:55.024899	f	2026-06-07 14:38:55.024899
76	11	f353ee3e3d322fb56d87a32746a80e78e4320e8699dc1bd48371fd47a56e5031	2026-06-14 14:41:15.680758	f	2026-06-07 14:41:15.680758
77	1	fb4dbc64f11bfc3b059a6c6eb6a78801975aca29d2bfa7e9a33350da216c3907	2026-06-14 14:41:20.260455	f	2026-06-07 14:41:20.260455
78	11	c109875e04e65c19e5852d2f82329db1c33ffe46db353f111964778b0aded6b7	2026-06-14 14:42:24.736494	f	2026-06-07 14:42:24.737566
79	19	9d0a0d70a6efaf1d34895e3ef823f2d1d0a600f2918f7bb70d7040e634c918e4	2026-06-14 14:43:36.648449	f	2026-06-07 14:43:36.649454
80	1	54cdf213976a186a018440276a8e7a0f723832728ab878fc1b2d8feaff9a6348	2026-06-14 14:46:10.153201	f	2026-06-07 14:46:10.153201
81	19	b9157a3296d02fca615be9333293b8b7b1a0dd126b03a198cadbdbc919c30b61	2026-06-14 14:50:51.925649	f	2026-06-07 14:50:51.925649
82	19	5ae7df88b4ad2162f2558218afba4ac34d37a6aaebbe259f3672b922cd030b24	2026-06-14 14:52:33.138812	f	2026-06-07 14:52:33.138812
83	1	354c69e6262dbc16367e031701d89bbc8062cac1db8e9ea714675d46a75a78c8	2026-06-14 14:55:09.264203	f	2026-06-07 14:55:09.264203
84	1	3bf3f1de2453f4003e4fd3e77ccb6f5bd9303a7d961c1a559e5891491ed8b222	2026-06-14 14:55:15.344416	f	2026-06-07 14:55:15.344416
85	19	41299d0c1264b027e88801615d06825bb166fb0d12a14fe975136b96a8a4e99a	2026-06-14 14:55:15.442416	f	2026-06-07 14:55:15.442416
86	1	d3a7844aa364e362cdc856dea045ec6aa9f798d399e0feebc957dd5aee21881d	2026-06-14 14:55:47.104099	f	2026-06-07 14:55:47.104099
87	1	30785969e6746433de3e784b0c9b0f75131067e41dee3289514572f60de51922	2026-06-14 14:56:31.51754	f	2026-06-07 14:56:31.51754
88	10	0bfb1fbda96b0c535bca4cf444eb00dcfa3b9133496bb3b2e78da007e04fc6e0	2026-06-14 14:57:30.613599	f	2026-06-07 14:57:30.613599
89	1	d6d7ff9c2f05b0cbc2c9a144fed47c1c594a168ef3abb105d7bbb41b5d76956a	2026-06-14 14:58:26.207556	f	2026-06-07 14:58:26.208614
90	11	ed2507109d450d6e635f0207882d89dde3db9394083b72a9c9862bb438774b28	2026-06-14 14:59:41.158305	f	2026-06-07 14:59:41.158305
91	19	c9592f971e88282f7f0f7873482bd1c5d75c9459c8c92d4f93a4866b7b82b6fe	2026-06-14 15:01:07.077856	f	2026-06-07 15:01:07.078857
92	19	51a52b455efcdf26f5a210b0ed2936862497acff832e32ee8c03a9e6eab8aa2d	2026-06-14 15:01:32.604197	f	2026-06-07 15:01:32.604197
93	11	0d01d2fd18dddd939daa7ee7a377f62eb12eff792d3c45430d59dc7a64f5042b	2026-06-14 15:02:41.036627	f	2026-06-07 15:02:41.036627
94	1	6e3e3cd959f41885f487904098240bbbf8cea57be21d1502134326855b5550de	2026-06-14 15:02:42.628771	f	2026-06-07 15:02:42.628771
95	1	94ca2df27b4ff6b1a6905f1cbfbf5f5114287f842f7bbbbbd615f802bad6015a	2026-06-14 15:06:01.325711	f	2026-06-07 15:06:01.325711
96	10	591f9c2192ec9abfe70c49e479b105e4a399ecb1b003d4b91ee48aacab8a5cfd	2026-06-14 15:06:38.851559	f	2026-06-07 15:06:38.851559
97	1	5ba8b99135707a3afc783455a0d7670343a331ba76f7386718620f83e4cff46e	2026-06-14 15:07:35.364316	f	2026-06-07 15:07:35.364316
98	11	7a8d570046ed72738dd0a3aa04a457c285a5ddbd676243cf21a7cd98fc63e19a	2026-06-14 15:09:43.210321	f	2026-06-07 15:09:43.210321
99	1	4d21ea1eedc0f82d17a2657c25ef70762b1017f195f8fe8bc4075c249485d60a	2026-06-14 15:10:36.049687	f	2026-06-07 15:10:36.049687
100	11	a984db840f030cd87bc991ffb610403e1f7b5e01d2698cfe98dcb872651b494a	2026-06-14 15:11:31.949012	f	2026-06-07 15:11:31.950014
101	1	04b020b402af2fa81b9d55d70f20866fb1c9ab111bf3167dd1535c1c56353960	2026-06-14 15:13:09.972088	f	2026-06-07 15:13:09.972088
102	1	86d634bc424cf78d8c2229ac5c6eaaaeeb148f7ee9299456ac103ef97df8527b	2026-06-14 15:13:58.385418	f	2026-06-07 15:13:58.385418
103	11	bd2e69ee00430802eab97940136ee699be8451ba25b4ca9be703514932cd67cd	2026-06-14 15:15:33.518337	f	2026-06-07 15:15:33.521365
104	11	dee207ce05e3e72efc3430d56217ed5c2f2645aeb9adfdc98b0f5220cec57d7b	2026-06-14 15:16:20.648876	f	2026-06-07 15:16:20.648876
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.roles (id, name, description, created_at, updated_at) FROM stdin;
1	ROLE_ADMIN	System administrator with full access	2026-06-05 10:44:23.492821	2026-06-05 10:44:23.492821
2	ROLE_OPERATOR	Utility operations staff	2026-06-05 10:44:23.541823	2026-06-05 10:44:23.541823
3	ROLE_FINANCE	Finance and billing staff	2026-06-05 10:44:23.54582	2026-06-05 10:44:23.54582
4	ROLE_CUSTOMER	End customer with self-service access	2026-06-05 10:44:23.54982	2026-06-05 10:44:23.54982
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tariffs (id, meter_type, rate, service_charge, vat, penalty_rate, version, effective_date, active, created_at, updated_at) FROM stdin;
1	WATER	450.0000	1500.0000	18.00	50.0000	1	2026-01-01	f	2026-06-05 11:15:47.729524	2026-06-05 11:15:47.729524
2	WATER	450.0000	1500.0000	18.00	50.0000	2	2026-01-01	f	2026-06-05 12:29:17.736084	2026-06-05 12:29:17.736084
3	WATER	500.0000	2000.0000	18.00	5.0000	3	2026-01-01	t	2026-06-07 14:34:34.390734	2026-06-07 14:34:34.390734
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_roles (id, user_id, role_id, created_at) FROM stdin;
1	1	1	2026-06-05 10:44:23.783823
9	9	4	2026-06-05 12:08:52.041972
10	10	2	2026-06-05 12:32:38.352737
11	11	3	2026-06-05 12:45:30.194343
13	13	4	2026-06-05 13:23:27.109134
15	15	4	2026-06-05 13:24:14.201485
16	16	4	2026-06-05 13:33:12.258869
17	17	4	2026-06-07 14:27:39.484296
19	19	4	2026-06-07 14:30:19.413629
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, email, password, first_name, last_name, enabled, created_at, updated_at, full_name, phone_number, email_verified, first_login) FROM stdin;
1	brillanteigabemurangwa	brillanteigabemurangwa@gmail.com	$2a$10$EacBgY18mBgvg/ucyz2uou4oFvmzu5AZmFaZEFhVlGWiRFy21Rw12	Brillante	Murangwa	t	2026-06-05 10:44:23.759822	2026-06-05 10:44:23.759822	Brillante Igabe Murangwa	+250780000000	t	f
9	uwasek21	uwasek21@gmail.com	$2a$10$OosTq4YvqvynzMYm9tFnuu8BP0x83noqQsmfF4JuduFEOR/xzYcBe	\N	\N	t	2026-06-05 12:08:52.013969	2026-06-05 12:09:49.804379	Uwase kevine	+250788999888	t	f
10	murangwabr	murangwabr@gmail.com	$2a$10$gzJwnKoO5j7pIaQU2ejoSOd6JozgcLcS5qNGEoRFUDpF8GnDj4KyK	\N	\N	t	2026-06-05 12:32:38.35082	2026-06-05 12:40:17.914097	Operator One	+250788111222	t	f
11	migzgloire	migzgloire@gmail.com	$2a$10$Jn0PYCKqRtZezQnncyPB7uJK6D6PlLLl3Gjeg97nMErBZFIc00Zfe	\N	\N	t	2026-06-05 12:45:30.193338	2026-06-05 12:47:21.951687	Finance One	+250788333444	t	f
13	mukundwaa35	mukundwaa35@gmail.comm	$2a$10$dsev68kVxK0Caodhtu8eseszf0/DZrQkoZcVNVPikCDYE.FDvKNum	\N	\N	f	2026-06-05 13:23:27.108134	2026-06-05 13:23:27.108134	aurore mukundwa	+250788923456	f	f
15	mukundwaa351	mukundwaa35@gmail.com	$2a$10$yCniptfd/kYhuvi5Wi7noexaImNH245bwJ6GnUFeHKQK6.Xnzoz9K	\N	\N	t	2026-06-05 13:24:14.199482	2026-06-05 13:25:02.56318	aurore mukundwa	+250788920456	t	f
16	whenever67e	whenever67e@gmail.com	$2a$10$wJ0j6X8P6DitmCMQAHpkDOGie8XFFCMCQlc5EIR.SqqevmxqJAuA.	\N	\N	t	2026-06-05 13:33:12.25587	2026-06-05 13:36:34.30774	Hellooooooo	+250788119922	t	f
17	awet.feseha1	awet.feseha1@gmail.com	$2a$10$auXfxmMbMqTzcoNjD1KcreOaijci8x7oBO7Y8No7DItJ5B31RwTny	\N	\N	f	2026-06-07 14:27:39.466261	2026-06-07 14:27:39.466261	Awet Feseha	+250787654321	f	f
19	awet.fesseha1	awet.fesseha1@gmail.com	$2a$10$dI97ZwvNvGlazNrslTAL0.RP8HOfwgiHY1z4UxqVjiY.IDTrbU/2S	\N	\N	t	2026-06-07 14:30:19.412628	2026-06-07 14:30:58.260255	Awet Feseha	+250787694321	t	f
\.


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 48, true);


--
-- Name: bills_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.bills_id_seq', 7, true);


--
-- Name: comments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.comments_id_seq', 1, false);


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customers_id_seq', 8, true);


--
-- Name: jwt_blacklist_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.jwt_blacklist_id_seq', 1, false);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.meter_readings_id_seq', 7, true);


--
-- Name: meters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.meters_id_seq', 4, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.notifications_id_seq', 20, true);


--
-- Name: otps_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.otps_id_seq', 8, true);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.password_reset_tokens_id_seq', 1, false);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payments_id_seq', 9, true);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refresh_tokens_id_seq', 104, true);


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.roles_id_seq', 4, true);


--
-- Name: tariffs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tariffs_id_seq', 3, true);


--
-- Name: user_roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_roles_id_seq', 19, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 19, true);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: bills bills_bill_reference_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_bill_reference_key UNIQUE (bill_reference);


--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: customers customers_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_email_key UNIQUE (email);


--
-- Name: customers customers_national_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_national_id_key UNIQUE (national_id);


--
-- Name: customers customers_phone_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_phone_number_key UNIQUE (phone_number);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: jwt_blacklist jwt_blacklist_jti_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_blacklist
    ADD CONSTRAINT jwt_blacklist_jti_key UNIQUE (jti);


--
-- Name: jwt_blacklist jwt_blacklist_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jwt_blacklist
    ADD CONSTRAINT jwt_blacklist_pkey PRIMARY KEY (id);


--
-- Name: meter_readings meter_readings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_pkey PRIMARY KEY (id);


--
-- Name: meters meters_meter_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_meter_number_key UNIQUE (meter_number);


--
-- Name: meters meters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: otps otps_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps
    ADD CONSTRAINT otps_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: tariffs tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_pkey PRIMARY KEY (id);


--
-- Name: bills uk_bills_meter_month_year; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk_bills_meter_month_year UNIQUE (meter_id, month, year);


--
-- Name: meter_readings uk_meter_readings_meter_month_year; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT uk_meter_readings_meter_month_year UNIQUE (meter_id, month, year);


--
-- Name: tariffs uk_tariffs_meter_type_version; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT uk_tariffs_meter_type_version UNIQUE (meter_type, version);


--
-- Name: user_roles uk_user_roles_user_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_logs_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);


--
-- Name: idx_audit_logs_entity_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity_id ON public.audit_logs USING btree (entity_id);


--
-- Name: idx_audit_logs_entity_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity_name ON public.audit_logs USING btree (entity_name);


--
-- Name: idx_audit_logs_performed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_performed_by ON public.audit_logs USING btree (performed_by);


--
-- Name: idx_audit_logs_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_timestamp ON public.audit_logs USING btree ("timestamp");


--
-- Name: idx_bills_bill_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bills_bill_reference ON public.bills USING btree (bill_reference);


--
-- Name: idx_bills_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bills_customer_id ON public.bills USING btree (customer_id);


--
-- Name: idx_bills_meter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bills_meter_id ON public.bills USING btree (meter_id);


--
-- Name: idx_bills_month_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bills_month_year ON public.bills USING btree (month, year);


--
-- Name: idx_bills_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bills_status ON public.bills USING btree (status);


--
-- Name: idx_comments_bill_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_bill_id ON public.comments USING btree (bill_id);


--
-- Name: idx_comments_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_created_at ON public.comments USING btree (created_at);


--
-- Name: idx_comments_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comments_user_id ON public.comments USING btree (user_id);


--
-- Name: idx_customers_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_email ON public.customers USING btree (email);


--
-- Name: idx_customers_full_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_full_name ON public.customers USING btree (full_name);


--
-- Name: idx_customers_national_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_national_id ON public.customers USING btree (national_id);


--
-- Name: idx_customers_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customers_status ON public.customers USING btree (status);


--
-- Name: idx_jwt_blacklist_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jwt_blacklist_expires_at ON public.jwt_blacklist USING btree (expires_at);


--
-- Name: idx_meter_readings_meter_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meter_readings_meter_id ON public.meter_readings USING btree (meter_id);


--
-- Name: idx_meter_readings_month_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meter_readings_month_year ON public.meter_readings USING btree (month, year);


--
-- Name: idx_meter_readings_reading_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meter_readings_reading_date ON public.meter_readings USING btree (reading_date);


--
-- Name: idx_meters_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meters_customer_id ON public.meters USING btree (customer_id);


--
-- Name: idx_meters_meter_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meters_meter_number ON public.meters USING btree (meter_number);


--
-- Name: idx_meters_meter_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meters_meter_type ON public.meters USING btree (meter_type);


--
-- Name: idx_meters_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_meters_status ON public.meters USING btree (status);


--
-- Name: idx_notifications_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (created_at);


--
-- Name: idx_notifications_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_customer_id ON public.notifications USING btree (customer_id);


--
-- Name: idx_notifications_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_status ON public.notifications USING btree (status);


--
-- Name: idx_otps_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otps_expires_at ON public.otps USING btree (expires_at);


--
-- Name: idx_otps_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_otps_user_id ON public.otps USING btree (user_id);


--
-- Name: idx_password_reset_tokens_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_expires_at ON public.password_reset_tokens USING btree (expires_at);


--
-- Name: idx_password_reset_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_user_id ON public.password_reset_tokens USING btree (user_id);


--
-- Name: idx_payments_bill_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_bill_id ON public.payments USING btree (bill_id);


--
-- Name: idx_payments_payment_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_payment_date ON public.payments USING btree (payment_date);


--
-- Name: idx_payments_payment_method; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payments_payment_method ON public.payments USING btree (payment_method);


--
-- Name: idx_refresh_tokens_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens USING btree (expires_at);


--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_roles_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_roles_name ON public.roles USING btree (name);


--
-- Name: idx_tariffs_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tariffs_active ON public.tariffs USING btree (active);


--
-- Name: idx_tariffs_effective_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tariffs_effective_date ON public.tariffs USING btree (effective_date);


--
-- Name: idx_tariffs_meter_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tariffs_meter_type ON public.tariffs USING btree (meter_type);


--
-- Name: idx_user_roles_role_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_role_id ON public.user_roles USING btree (role_id);


--
-- Name: idx_user_roles_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_roles_user_id ON public.user_roles USING btree (user_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_phone_number; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_users_phone_number ON public.users USING btree (phone_number) WHERE (phone_number IS NOT NULL);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: uk_notifications_customer_event_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_notifications_customer_event_reference ON public.notifications USING btree (customer_id, event_type, reference_id) WHERE ((event_type IS NOT NULL) AND (reference_id IS NOT NULL));


--
-- Name: bills trg_bill_approved; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_bill_approved AFTER UPDATE OF approved ON public.bills FOR EACH ROW EXECUTE FUNCTION public.notify_bill_approved();


--
-- Name: bills trg_bill_paid; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_bill_paid AFTER UPDATE OF status ON public.bills FOR EACH ROW EXECUTE FUNCTION public.notify_bill_paid();


--
-- Name: bills fk_bills_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE RESTRICT;


--
-- Name: bills fk_bills_meter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_meter FOREIGN KEY (meter_id) REFERENCES public.meters(id) ON DELETE RESTRICT;


--
-- Name: bills fk_bills_meter_reading; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_meter_reading FOREIGN KEY (meter_reading_id) REFERENCES public.meter_readings(id) ON DELETE RESTRICT;


--
-- Name: bills fk_bills_tariff; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk_bills_tariff FOREIGN KEY (tariff_id) REFERENCES public.tariffs(id) ON DELETE RESTRICT;


--
-- Name: comments fk_comments_bill; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comments_bill FOREIGN KEY (bill_id) REFERENCES public.bills(id) ON DELETE CASCADE;


--
-- Name: comments fk_comments_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: meter_readings fk_meter_readings_meter; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fk_meter_readings_meter FOREIGN KEY (meter_id) REFERENCES public.meters(id) ON DELETE RESTRICT;


--
-- Name: meters fk_meters_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fk_meters_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE RESTRICT;


--
-- Name: notifications fk_notifications_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk_notifications_customer FOREIGN KEY (customer_id) REFERENCES public.customers(id) ON DELETE CASCADE;


--
-- Name: otps fk_otps_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otps
    ADD CONSTRAINT fk_otps_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: password_reset_tokens fk_password_reset_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: payments fk_payments_bill; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk_payments_bill FOREIGN KEY (bill_id) REFERENCES public.bills(id) ON DELETE RESTRICT;


--
-- Name: refresh_tokens fk_refresh_tokens_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_roles fk_user_roles_role; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES public.roles(id) ON DELETE CASCADE;


--
-- Name: user_roles fk_user_roles_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict HmsYCaokvTQsjgx6CA6P8Xo1csLkbhAxONLu3kR944QKFV5BtMqUOjoarFc4qAW


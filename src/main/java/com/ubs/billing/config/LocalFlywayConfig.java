package com.ubs.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local development Flyway strategy for recovering from inconsistent dev databases.
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalFlywayConfig {

    @Bean
    public FlywayMigrationStrategy localFlywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);

            if (isSchemaInconsistent(jdbc)) {
                recreatePublicSchema(jdbc);
            } else {
                flyway.repair();
                syncMissingHistoryEntries(jdbc);
                flyway.repair();
            }

            flyway.migrate();
        };
    }

    private boolean isSchemaInconsistent(JdbcTemplate jdbc) {
        boolean rolesExists = tableExists(jdbc, "roles");
        if (rolesExists) {
            return false;
        }

        boolean flywayHistoryExists = tableExists(jdbc, "flyway_schema_history");
        boolean laterArtifactsExist = tableExists(jdbc, "customers")
                || tableExists(jdbc, "bills")
                || tableExists(jdbc, "password_reset_tokens")
                || tableExists(jdbc, "otps");

        return flywayHistoryExists || laterArtifactsExist;
    }

    private void recreatePublicSchema(JdbcTemplate jdbc) {
        log.warn("Recreating public schema because core auth tables are missing but Flyway/billing artifacts already exist");
        jdbc.execute("DROP SCHEMA public CASCADE");
        jdbc.execute("CREATE SCHEMA public");
        jdbc.execute("GRANT ALL ON SCHEMA public TO postgres");
        jdbc.execute("GRANT ALL ON SCHEMA public TO public");
    }

    private void syncMissingHistoryEntries(JdbcTemplate jdbc) {
        if (!tableExists(jdbc, "flyway_schema_history")) {
            return;
        }

        Integer currentVersion = jdbc.query(
                "SELECT COALESCE(MAX(version::int), 0) FROM flyway_schema_history WHERE success = true",
                rs -> rs.next() ? rs.getInt(1) : 0);

        if (currentVersion == null) {
            currentVersion = 0;
        }

        Map<Integer, String> pendingChecks = new LinkedHashMap<>();
        pendingChecks.put(8, tableExistsSql("password_reset_tokens"));
        pendingChecks.put(9, tableExistsSql("customers"));
        pendingChecks.put(10, tableExistsSql("meters"));
        pendingChecks.put(11, tableExistsSql("meter_readings"));
        pendingChecks.put(12, tableExistsSql("tariffs"));
        pendingChecks.put(13, tableExistsSql("bills"));
        pendingChecks.put(14, tableExistsSql("payments"));
        pendingChecks.put(15, tableExistsSql("notifications"));
        pendingChecks.put(16, "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'notify_bill_generated')");
        pendingChecks.put(17, tableExistsSql("comments"));
        pendingChecks.put(18, tableExistsSql("audit_logs"));
        pendingChecks.put(19, columnExistsSql("users", "first_login"));
        pendingChecks.put(20, columnExistsSql("audit_logs", "old_value"));
        pendingChecks.put(21, columnExistsSql("bills", "due_date"));
        pendingChecks.put(22, columnExistsSql("notifications", "event_type"));
        pendingChecks.put(23, "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_bill_approved')");

        for (Map.Entry<Integer, String> entry : pendingChecks.entrySet()) {
            int version = entry.getKey();
            if (version <= currentVersion) {
                continue;
            }

            Boolean exists = jdbc.queryForObject(entry.getValue(), Boolean.class);
            if (!Boolean.TRUE.equals(exists)) {
                break;
            }

            markVersionApplied(jdbc, version);
            currentVersion = version;
            log.info("Marked Flyway migration V{} as applied (schema already present)", version);
        }
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Boolean exists = jdbc.queryForObject(tableExistsSql(tableName), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private String tableExistsSql(String tableName) {
        return "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = '" + tableName + "')";
    }

    private String columnExistsSql(String tableName, String columnName) {
        return "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = '" + tableName
                + "' AND column_name = '" + columnName + "')";
    }

    private void markVersionApplied(JdbcTemplate jdbc, int version) {
        String script = resolveScriptName(version);
        String description = resolveDescription(version);

        jdbc.update("""
                INSERT INTO flyway_schema_history
                    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
                SELECT COALESCE(MAX(installed_rank), 0) + 1, ?, ?, 'SQL', ?, 0, 'local-sync', NOW(), 0, TRUE
                FROM flyway_schema_history
                WHERE NOT EXISTS (
                    SELECT 1 FROM flyway_schema_history WHERE version = ?
                )
                """, String.valueOf(version), description, script, String.valueOf(version));
    }

    private String resolveScriptName(int version) {
        return switch (version) {
            case 8 -> "V8__create_password_reset_tokens_table.sql";
            case 9 -> "V9__create_customers_table.sql";
            case 10 -> "V10__create_meters_table.sql";
            case 11 -> "V11__create_meter_readings_table.sql";
            case 12 -> "V12__create_tariffs_table.sql";
            case 13 -> "V13__create_bills_table.sql";
            case 14 -> "V14__create_payments_table.sql";
            case 15 -> "V15__create_notifications_table.sql";
            case 16 -> "V16__create_notification_triggers.sql";
            case 17 -> "V17__create_comments_table.sql";
            case 18 -> "V18__create_audit_logs_table.sql";
            case 19 -> "V19__extend_users_first_login.sql";
            case 20 -> "V20__extend_audit_logs_values.sql";
            case 21 -> "V21__extend_bills_due_date.sql";
            case 22 -> "V22__extend_notifications_event_type.sql";
            case 23 -> "V23__update_notification_triggers.sql";
            default -> "V" + version + "__migration.sql";
        };
    }

    private String resolveDescription(int version) {
        return switch (version) {
            case 8 -> "create password reset tokens table";
            case 9 -> "create customers table";
            case 10 -> "create meters table";
            case 11 -> "create meter readings table";
            case 12 -> "create tariffs table";
            case 13 -> "create bills table";
            case 14 -> "create payments table";
            case 15 -> "create notifications table";
            case 16 -> "create notification triggers";
            case 17 -> "create comments table";
            case 18 -> "create audit logs table";
            case 19 -> "extend users first login";
            case 20 -> "extend audit logs values";
            case 21 -> "extend bills due date";
            case 22 -> "extend notifications event type";
            case 23 -> "update notification triggers";
            default -> "migration " + version;
        };
    }
}

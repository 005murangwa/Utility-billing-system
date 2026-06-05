package com.ubs.billing.util;

public final class AuditValueFormatter {

    private AuditValueFormatter() {
    }

    public static String format(String key, Object value) {
        if (value == null) {
            return null;
        }
        String escaped = String.valueOf(value).replace("\"", "\\\"");
        return "{\"" + key + "\":\"" + escaped + "\"}";
    }

    public static String formatRoles(java.util.Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        String joined = String.join(",", roles);
        return "{\"roles\":\"" + joined + "\"}";
    }
}

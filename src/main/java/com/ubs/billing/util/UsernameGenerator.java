package com.ubs.billing.util;

public final class UsernameGenerator {

    private UsernameGenerator() {
    }

    public static String fromEmail(String email) {
        int atIndex = email.indexOf('@');
        String base = atIndex > 0 ? email.substring(0, atIndex) : email;
        base = base.replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            return "user";
        }
        return base.length() > 50 ? base.substring(0, 50) : base;
    }

    public static String withSuffix(String baseUsername, int suffix) {
        String suffixValue = String.valueOf(suffix);
        int maxBaseLength = 50 - suffixValue.length();
        String trimmedBase = baseUsername.length() > maxBaseLength
                ? baseUsername.substring(0, maxBaseLength)
                : baseUsername;
        return trimmedBase + suffixValue;
    }
}

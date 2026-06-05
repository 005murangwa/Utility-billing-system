package com.ubs.billing.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalizeRwandaPhone(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String normalized = phoneNumber.replaceAll("\\s+", "");
        if (normalized.startsWith("+250")) {
            return normalized;
        }
        if (normalized.startsWith("250")) {
            return "+" + normalized;
        }
        if (normalized.startsWith("0")) {
            return "+250" + normalized.substring(1);
        }
        if (normalized.startsWith("7")) {
            return "+250" + normalized;
        }
        return normalized;
    }
}

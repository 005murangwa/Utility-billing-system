package com.ubs.billing.smoke.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SmokeTestData {

    public static final String ADMIN_EMAIL = "brillanteigabemurangwa@gmail.com";
    public static final String ADMIN_PASSWORD = "Password123!";
    public static final String STAFF_PASSWORD = "Password123!";

    private SmokeTestData() {
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@smoke.test";
    }

    public static String uniquePhone() {
        int suffix = ThreadLocalRandom.current().nextInt(100_000, 999_999);
        return "0788" + suffix;
    }

    public static String uniqueNationalId() {
        long value = ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 9_000_000_000_000_000L);
        return String.format("%016d", value);
    }

    public static String uniqueMeterNumber() {
        return "WM-SMOKE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

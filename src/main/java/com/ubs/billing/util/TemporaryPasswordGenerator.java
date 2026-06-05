package com.ubs.billing.util;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*!";
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
    private static final int MIN_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        char[] password = new char[MIN_LENGTH];
        password[0] = randomChar(UPPERCASE);
        password[1] = randomChar(LOWERCASE);
        password[2] = randomChar(DIGITS);
        password[3] = randomChar(SPECIAL);

        for (int i = 4; i < MIN_LENGTH; i++) {
            password[i] = randomChar(ALL);
        }

        shuffle(password);
        return new String(password);
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }

    private static void shuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}

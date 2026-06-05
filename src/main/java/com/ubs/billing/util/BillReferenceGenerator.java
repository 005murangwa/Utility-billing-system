package com.ubs.billing.util;

public final class BillReferenceGenerator {

    private BillReferenceGenerator() {
    }

    public static String generate(int year, int month, Long meterId, long sequence) {
        return String.format("BILL-%d%02d-M%05d-%04d", year, month, meterId, sequence);
    }
}

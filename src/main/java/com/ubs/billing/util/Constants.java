package com.ubs.billing.util;

public final class Constants {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_OPERATOR = "ROLE_OPERATOR";
    public static final String ROLE_FINANCE = "ROLE_FINANCE";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    public static final String[] DEFAULT_ROLES = {
            ROLE_ADMIN,
            ROLE_OPERATOR,
            ROLE_FINANCE,
            ROLE_CUSTOMER
    };

    private Constants() {
    }
}

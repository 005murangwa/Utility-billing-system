package com.ubs.billing.util;

import org.springframework.util.StringUtils;

public final class SearchQueryUtils {

    private SearchQueryUtils() {
    }

    /**
     * Returns an empty string when the filter is absent so PostgreSQL does not
     * treat null JPQL parameters as bytea in LOWER/LIKE expressions.
     */
    public static String toOptionalLikePattern(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}

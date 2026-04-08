package com.vpvpteam.xmlinvoicevalidationbackend.util;

import java.util.Collection;

public final class FieldCheck {

    private FieldCheck() {}

    /**
     * Null/blank string check.
     */
    public static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Empty collection check.
     */
    public static <T> boolean notEmpty(Collection<T> value) {
        return value != null && !value.isEmpty();
    }
}
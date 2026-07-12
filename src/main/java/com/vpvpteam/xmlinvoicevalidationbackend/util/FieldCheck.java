package com.vpvpteam.xmlinvoicevalidationbackend.util;

import java.util.Collection;

public final class FieldCheck {
    private FieldCheck() {}

    public static boolean notNullNorBlank(String value) {
        return value != null && !value.isBlank();
    }
    public static <T> boolean notEmpty(Collection<T> value) {
        return value != null && !value.isEmpty();
    }
}
package com.vpvpteam.xmlinvoicevalidationbackend.util;

public final class FieldCheck {
    private FieldCheck() {}

    public static boolean notNullNorBlank(String value) {
        return value != null && !value.isBlank();
    }
}
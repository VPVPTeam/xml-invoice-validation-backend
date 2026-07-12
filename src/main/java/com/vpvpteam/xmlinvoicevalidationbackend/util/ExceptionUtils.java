package com.vpvpteam.xmlinvoicevalidationbackend.util;

public final class ExceptionUtils {
    private ExceptionUtils() {}

    public static String safeMessage(Exception ex) {
        if (ex == null) return "UnknownException";
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }
}
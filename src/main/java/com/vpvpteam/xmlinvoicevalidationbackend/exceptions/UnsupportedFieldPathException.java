package com.vpvpteam.xmlinvoicevalidationbackend.exceptions;

public final class UnsupportedFieldPathException extends RuntimeException {
    public UnsupportedFieldPathException(String fieldPath) {
        super("Unsupported field path: " + fieldPath);
    }
}

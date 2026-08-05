package com.vpvpteam.xmlinvoicevalidationbackend.exceptions;

public final class EmptyBatchException extends RuntimeException {
    public EmptyBatchException(String message) {
        super(message);
    }
}
package com.vpvpteam.xmlinvoicevalidationbackend.exceptions;

public final class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
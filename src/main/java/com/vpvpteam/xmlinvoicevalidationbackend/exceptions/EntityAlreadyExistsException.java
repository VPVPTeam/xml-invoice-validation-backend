package com.vpvpteam.xmlinvoicevalidationbackend.exceptions;

public final class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
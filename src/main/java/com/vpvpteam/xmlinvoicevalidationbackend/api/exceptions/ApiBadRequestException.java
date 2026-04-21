package com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions;

public class ApiBadRequestException extends RuntimeException {
    public ApiBadRequestException(String message) {
        super(message);
    }
}
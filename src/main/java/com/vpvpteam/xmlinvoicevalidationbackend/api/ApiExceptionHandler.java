// TODO: ПРОВЕРИТЬ НАХУЙ НУЖОН
package com.vpvpteam.xmlinvoicevalidationbackend.api;

import com.vpvpteam.xmlinvoicevalidationbackend.api.exceptions.ApiBadRequestException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiExceptionHandler {

    @ExceptionHandler(ApiBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(ApiBadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error. Something went wrong"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EntityAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)    // 409
                .body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {}
}
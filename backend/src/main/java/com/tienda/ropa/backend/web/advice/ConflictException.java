package com.tienda.ropa.backend.web.advice;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

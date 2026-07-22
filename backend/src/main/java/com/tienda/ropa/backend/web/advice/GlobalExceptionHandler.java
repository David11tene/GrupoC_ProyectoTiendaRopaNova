package com.tienda.ropa.backend.web.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Manejo global de excepciones
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Error 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NotFoundException ex
    ) {

        log.warn("NotFoundException: {}", ex.getMessage());

        return buildError(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // Error 409
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException ex
    ) {

        log.warn("ConflictException: {}", ex.getMessage());

        return buildError(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    // Error 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(
                    fe.getField(),
                    fe.getDefaultMessage()
            );
        }

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST,
                "Validación fallida. Revisa los campos enviados."
        );

        body.put("fields", fieldErrors);

        log.warn(
                "ValidationException - campos inválidos: {}",
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    // Error 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex
    ) {

        log.error(
                "Error inesperado: {}",
                ex.getMessage(),
                ex
        );

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Contacta al administrador."
        );
    }

    // Construye respuesta de error
    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String message
    ) {

        return ResponseEntity
                .status(status)
                .body(buildErrorBody(status, message));
    }

    // Construye cuerpo del error
    private Map<String, Object> buildErrorBody(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return body;
    }
}
package com.commerceflow.auth.web;

import com.commerceflow.auth.application.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiError> auth(AuthException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getCode(), exception.getMessage(), request, Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(item -> fields.putIfAbsent(item.getField(), item.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_ERROR", "Request validation failed", request, fields));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "An unexpected error occurred", request, Map.of()));
    }

    private ApiError error(String code, String message, HttpServletRequest request, Map<String, String> fields) {
        return new ApiError(Instant.now(), code, message, request.getRequestURI(),
                request.getHeader("X-Correlation-ID"), fields);
    }

    record ApiError(Instant timestamp, String code, String message, String path,
                    String correlationId, Map<String, String> fields) { }
}

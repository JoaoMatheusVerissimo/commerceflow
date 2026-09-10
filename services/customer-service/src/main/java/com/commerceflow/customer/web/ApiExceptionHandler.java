package com.commerceflow.customer.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrors.Body> status(ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        return ResponseEntity.status(status).body(ApiErrors.body(status, "CUSTOMER_NOT_FOUND",
                "Customer profile not found", request, List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrors.Body> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var fields = exception.getBindingResult().getFieldErrors().stream()
                .map(item -> new ApiErrors.FieldError(item.getField(), item.getCode(), item.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ApiErrors.body(400, "VALIDATION_ERROR",
                "Request validation failed", request, fields));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrors.Body> invalid(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiErrors.body(400, "INVALID_REQUEST",
                "Request contains an invalid value", request, List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrors.Body> unexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(ApiErrors.body(500, "INTERNAL_ERROR",
                "An unexpected error occurred", request, List.of()));
    }
}

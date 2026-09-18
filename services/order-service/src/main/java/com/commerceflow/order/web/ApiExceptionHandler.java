package com.commerceflow.order.web;

import com.commerceflow.order.CartException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CartException.class)
    ResponseEntity<ApiErrors.Body> domain(CartException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrors.Body> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiErrors.FieldError(f.getField(), f.getCode(), f.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiErrors.body(400, "VALIDATION_ERROR",
                "Request validation failed", request, fields));
    }
    @ExceptionHandler({IllegalArgumentException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrors.Body> invalid(Exception ex, HttpServletRequest request) {
        return response(400, "INVALID_REQUEST", "Invalid request value", request);
    }
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiErrors.Body> conflict(Exception ex, HttpServletRequest request) {
        return response(409, "RESOURCE_CONFLICT", "Concurrent change; retry the same command or reload", request);
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ResponseEntity<ApiErrors.Body> status(org.springframework.web.server.ResponseStatusException ex,
                                          HttpServletRequest request) {
        return response(ex.getStatusCode().value(), "ORDER_REQUEST_FAILED", ex.getReason(), request);
    }
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    ResponseEntity<ApiErrors.Body> missing(Exception ex, HttpServletRequest request) {
        return response(400, "IDEMPOTENCY_KEY_REQUIRED", "A UUID Idempotency-Key is required", request);
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrors.Body> unexpected(Exception ex, HttpServletRequest request) {
        return response(500, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }
    private ResponseEntity<ApiErrors.Body> response(int status, String code, String message,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiErrors.body(status, code, message, request, List.of()));
    }
}

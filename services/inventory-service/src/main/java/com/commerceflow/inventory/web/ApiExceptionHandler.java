package com.commerceflow.inventory.web;

import com.commerceflow.inventory.InventoryException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InventoryException.class)
    ResponseEntity<ApiErrors.Body> domain(InventoryException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(ApiErrors.body(ex.getStatus(), ex.getCode(),
                ex.getMessage(), request, List.of()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrors.Body> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiErrors.FieldError(f.getField(), f.getCode(), f.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiErrors.body(400, "VALIDATION_ERROR",
                "Request validation failed", request, fields));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrors.Body> unexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(ApiErrors.body(500, "INTERNAL_ERROR",
                "An unexpected error occurred", request, List.of()));
    }
}

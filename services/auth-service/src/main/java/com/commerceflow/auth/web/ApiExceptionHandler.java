package com.commerceflow.auth.web;

import com.commerceflow.auth.application.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiErrors.Body> auth(AuthException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getStatus().value(), exception.getCode(), exception.getMessage(),
                        request, Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrors.Body> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(item -> fields.putIfAbsent(item.getField(), item.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(error(400, "VALIDATION_ERROR", "Request validation failed", request, fields));
    }

    @ExceptionHandler({ServletRequestBindingException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrors.Body> requestBinding(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error(400, "INVALID_REQUEST", "Request is missing a value or contains invalid JSON",
                        request, Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrors.Body> unexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(500, "INTERNAL_ERROR", "An unexpected error occurred", request, Map.of()));
    }

    private ApiErrors.Body error(int status, String code, String message, HttpServletRequest request,
                                 Map<String, String> fields) {
        return ApiErrors.body(status, code, message, request, fields.entrySet().stream()
                .map(item -> new ApiErrors.FieldError(item.getKey(), "INVALID_VALUE", item.getValue())).toList());
    }

}

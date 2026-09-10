package com.commerceflow.catalog.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiErrors extends OncePerRequestFilter {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id;
        try {
            id = UUID.fromString(request.getHeader("X-Correlation-Id")).toString();
        } catch (RuntimeException exception) {
            id = UUID.randomUUID().toString();
        }
        request.setAttribute("correlationId", id);
        response.setHeader("X-Correlation-Id", id);
        chain.doFilter(request, response);
    }

    public static Body body(int status, String code, String message, HttpServletRequest request,
                            List<FieldError> fields) {
        return new Body(Instant.now().toString(), status, code, message, request.getRequestURI(),
                String.valueOf(request.getAttribute("correlationId")), fields);
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, int status,
                             String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(JSON.writeValueAsString(body(status, code, message, request, List.of())));
    }

    public record FieldError(String field, String code, String message) { }
    public record Body(String timestamp, int status, String code, String message, String path,
                       String correlationId, List<FieldError> fieldErrors) { }
}

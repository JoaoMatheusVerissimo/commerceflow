package com.commerceflow.gateway.web;

import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requested = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        String correlationId = isValid(requested) ? requested : UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER_NAME, correlationId).build();
        exchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isValid(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }
}

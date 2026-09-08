package com.commerceflow.gateway.web;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-90)
public class AuthRateLimitFilter implements WebFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh");

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int requestLimit;
    private final Duration windowDuration;

    public AuthRateLimitFilter(
            Clock clock,
            @Value("${commerceflow.security.auth-rate-limit.requests:10}") int requestLimit,
            @Value("${commerceflow.security.auth-rate-limit.window:PT1M}") Duration windowDuration) {
        this.clock = clock;
        this.requestLimit = requestLimit;
        this.windowDuration = windowDuration;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!LIMITED_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        Instant now = clock.instant();
        String client = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String key = client + ':' + path;
        AtomicBoolean allowed = new AtomicBoolean();
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.startedAt().plus(windowDuration))) {
                allowed.set(true);
                return new Window(now, 1);
            }
            allowed.set(existing.count() < requestLimit);
            return allowed.get() ? new Window(existing.startedAt(), existing.count() + 1) : existing;
        });

        if (allowed.get()) {
            cleanupExpired(now);
            return chain.filter(exchange);
        }

        long retryAfter = Math.max(1, Duration.between(now, current.startedAt().plus(windowDuration)).toSeconds());
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", Long.toString(retryAfter));
        String correlationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        String body = "{\"timestamp\":\"" + now + "\",\"status\":429,"
                + "\"code\":\"AUTH_RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Too many authentication attempts\","
                + "\"path\":\"" + path + "\","
                + "\"correlationId\":\"" + (correlationId == null ? "unknown" : correlationId) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private void cleanupExpired(Instant now) {
        if (windows.size() > 1_000) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().startedAt().plus(windowDuration)));
        }
    }

    private record Window(Instant startedAt, int count) {
    }
}

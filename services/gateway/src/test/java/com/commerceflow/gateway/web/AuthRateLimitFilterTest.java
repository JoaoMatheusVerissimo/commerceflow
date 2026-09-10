package com.commerceflow.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {
    @Test
    void blocksExcessAttemptsButDoesNotLimitUnrelatedRoutes() {
        var filter = new AuthRateLimitFilter(Clock.fixed(Instant.now(), ZoneOffset.UTC), 1, Duration.ofMinutes(1));
        var first = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login"));
        filter.filter(first, exchange -> Mono.empty()).block();
        assertThat(first.getResponse().getStatusCode()).isNull();
        var second = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login"));
        filter.filter(second, exchange -> Mono.empty()).block();
        assertThat(second.getResponse().getStatusCode().value()).isEqualTo(429);
        assertThat(second.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        var other = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers/me"));
        filter.filter(other, exchange -> Mono.empty()).block();
        assertThat(other.getResponse().getStatusCode()).isNull();
    }
}

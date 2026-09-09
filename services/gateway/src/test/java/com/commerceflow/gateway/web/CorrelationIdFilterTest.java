package com.commerceflow.gateway.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    @Test
    void createsAndPropagatesACorrelationId() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers/me"));
        var propagated = new AtomicReference<String>();
        new CorrelationIdFilter().filter(exchange, filtered -> {
            propagated.set(filtered.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME));
            return Mono.empty();
        }).block();

        assertThat(propagated.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo(propagated.get());
    }
}

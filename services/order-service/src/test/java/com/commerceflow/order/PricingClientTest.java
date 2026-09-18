package com.commerceflow.order;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingClientTest {
    @Test
    void forwardsIdentityAndCorrelationAndHandlesUnavailableAndIneligible() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var responseStatus = new java.util.concurrent.atomic.AtomicInteger(200);
        var authorization = new java.util.concurrent.atomic.AtomicReference<String>();
        var correlation = new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/internal/pricing/quote", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            correlation.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            byte[] body = "{\"total\":\"90.00\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var client = new PricingClient("http://127.0.0.1:" + server.getAddress().getPort());
            var cart = new CartService.Cart(1, List.of(new CartService.Item(UUID.randomUUID(), "SKU", 1)), null);
            assertThat(client.quote(cart, "test-token", "correlation").total()).isEqualTo("90.00");
            assertThat(authorization.get()).isEqualTo("Bearer test-token");
            assertThat(correlation.get()).isEqualTo("correlation");
            responseStatus.set(422);
            assertThatThrownBy(() -> client.quote(cart, "test-token", "correlation"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                            assertThat(e.getStatusCode().value()).isEqualTo(422));
            responseStatus.set(500);
            assertThatThrownBy(() -> client.quote(cart, "test-token", "correlation"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                            assertThat(e.getStatusCode().value()).isEqualTo(503));
        } finally { server.stop(0); }
    }
}

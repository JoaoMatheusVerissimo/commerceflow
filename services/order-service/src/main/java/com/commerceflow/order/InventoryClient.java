package com.commerceflow.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class InventoryClient {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI endpoint;
    public InventoryClient(@Value("${commerceflow.inventory-url}") String url) {
        endpoint = URI.create(url + "/internal/reservations");
    }
    public record Item(String sku, int quantity) { }
    public record Request(UUID orderId, List<Item> items) { }
    public record Reservation(UUID orderId, UUID reservationId, String status,
                              OffsetDateTime expiresAt, List<Item> items) { }
    public Reservation reserve(Request payload, String token, String correlation) {
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                    .header("X-Correlation-Id", correlation)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 422) { throw new InventoryRejectedException(); }
            if (response.statusCode() != 200) { throw new InventoryUnavailableException(); }
            return JSON.readValue(response.body(), Reservation.class);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new InventoryUnavailableException();
        } catch (java.io.IOException ex) { throw new InventoryUnavailableException(); }
    }
    public static class InventoryRejectedException extends RuntimeException { }
    public static class InventoryUnavailableException extends RuntimeException { }
}

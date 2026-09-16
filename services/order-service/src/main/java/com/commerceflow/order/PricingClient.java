package com.commerceflow.order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PricingClient {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI endpoint;
    public PricingClient(@Value("${commerceflow.catalog-url}") String url) {
        endpoint = URI.create(url + "/internal/pricing/quote");
    }
    public JsonNode quote(CartService.Cart cart, String token, String correlation) {
        if (cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cart is empty");
        }
        var payload = new java.util.HashMap<String, Object>(Map.of("items", cart.items()));
        payload.put("coupon", cart.coupon());
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                    .header("X-Correlation-Id", correlation)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload))).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 422) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Product, variant or coupon is unavailable or ineligible; review the cart");
            }
            if (response.statusCode() != 200) { throw unavailable(); }
            return JSON.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw unavailable();
        } catch (java.io.IOException ex) { throw unavailable(); }
    }
    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Pricing temporarily unavailable; try again");
    }
}

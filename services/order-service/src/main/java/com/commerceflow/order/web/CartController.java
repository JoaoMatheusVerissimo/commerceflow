package com.commerceflow.order.web;

import com.commerceflow.order.CartService;
import com.commerceflow.order.PricingClient;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class CartController {
    private final CartService carts;
    private final PricingClient pricing;
    public CartController(CartService carts, PricingClient pricing) { this.carts = carts; this.pricing = pricing; }
    @GetMapping("/cart")
    public CartService.Cart get(@AuthenticationPrincipal Jwt jwt) { return carts.get(owner(jwt)); }
    @PutMapping("/cart")
    public CartService.Cart replace(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CartService.Change change) { return carts.replace(owner(jwt), key, change); }
    @PostMapping("/cart/quote")
    public JsonNode quote(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return pricing.quote(carts.get(owner(jwt)), jwt.getTokenValue(),
                String.valueOf(request.getAttribute("correlationId")));
    }
    private static UUID owner(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}

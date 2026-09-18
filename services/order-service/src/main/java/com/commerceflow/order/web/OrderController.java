package com.commerceflow.order.web;

import com.commerceflow.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }
    @PostMapping("/checkout")
    public ResponseEntity<OrderService.Order> checkout(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody OrderService.Checkout checkout,
            HttpServletRequest request) {
        var order = orders.checkout(owner(jwt), key, checkout, jwt.getTokenValue(),
                String.valueOf(request.getAttribute("correlationId")));
        return ResponseEntity.accepted().body(order);
    }
    @GetMapping("/orders")
    public OrderService.Page history(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.history(owner(jwt), page, size);
    }
    @GetMapping("/orders/{id}")
    public OrderService.Order detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return orders.get(id, owner(jwt), isStaff(jwt));
    }
    @GetMapping("/admin/orders")
    public OrderService.Page admin(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return orders.admin(page, size); }
    private static UUID owner(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private static boolean isStaff(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.stream().anyMatch(r -> r.equals("SELLER") || r.equals("MANAGER")
                || r.equals("ADMIN"));
    }
}

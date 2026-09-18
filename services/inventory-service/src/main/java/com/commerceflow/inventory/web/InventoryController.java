package com.commerceflow.inventory.web;

import com.commerceflow.inventory.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {
    private final InventoryService inventory;
    public InventoryController(InventoryService inventory) { this.inventory = inventory; }
    @GetMapping("/availability/{sku}") public InventoryService.Stock availability(@PathVariable String sku) {
        return inventory.get(sku);
    }
    @PostMapping("/internal/reservations")
    public InventoryService.Reservation reserve(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InventoryService.ReserveRequest request) {
        return inventory.reserve(UUID.fromString(jwt.getSubject()), request);
    }
    @GetMapping("/admin/inventory") public List<InventoryService.Stock> list() { return inventory.list(); }
    @PostMapping("/admin/inventory/{sku}/movements")
    public InventoryService.Stock move(@PathVariable String sku, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InventoryService.Movement input) {
        return inventory.move(sku, input, UUID.fromString(jwt.getSubject()));
    }
    @PatchMapping("/admin/inventory/{sku}/minimum")
    public InventoryService.Stock minimum(@PathVariable String sku,
            @Valid @RequestBody InventoryService.Minimum input) { return inventory.minimum(sku, input); }
}

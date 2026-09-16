package com.commerceflow.catalog.web;

import com.commerceflow.catalog.application.PricingService;
import com.commerceflow.catalog.application.CatalogDtos.ProductView;
import com.commerceflow.catalog.application.CatalogException;
import com.commerceflow.catalog.domain.ProductRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PricingController {
    private final PricingService pricing;
    private final ProductRepository products;
    public PricingController(PricingService pricing, ProductRepository products) {
        this.pricing = pricing; this.products = products;
    }
    @PostMapping("/internal/pricing/quote")
    @SecurityRequirement(name = "bearerAuth")
    public PricingService.Quote quote(@Valid @RequestBody PricingService.Request request) {
        return pricing.quote(request);
    }
    @GetMapping("/admin/coupons/{code}")
    @SecurityRequirement(name = "bearerAuth")
    public PricingService.Coupon coupon(@PathVariable String code) {
        return pricing.coupons(code).stream().findFirst()
                .orElseThrow(() -> new CatalogException(404, "COUPON_NOT_FOUND", "Coupon not found"));
    }
    @PutMapping("/admin/coupons")
    @SecurityRequirement(name = "bearerAuth")
    public PricingService.Coupon save(@Valid @RequestBody PricingService.Coupon coupon,
                                     @AuthenticationPrincipal Jwt jwt) {
        return pricing.save(coupon, UUID.fromString(jwt.getSubject()));
    }
    @GetMapping("/products/by-id/{id}")
    @Transactional(readOnly = true)
    public ProductView product(@PathVariable UUID id) {
        return products.findById(id).filter(p -> "ACTIVE".equals(p.getStatus())).map(ProductView::from)
                .orElseThrow(() -> new CatalogException(404, "PRODUCT_NOT_FOUND", "Product not found"));
    }
}

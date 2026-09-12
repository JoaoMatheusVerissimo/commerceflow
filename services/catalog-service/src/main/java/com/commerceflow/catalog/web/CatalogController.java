package com.commerceflow.catalog.web;

import com.commerceflow.catalog.application.CatalogDtos.CategoryView;
import com.commerceflow.catalog.application.CatalogDtos.ProductView;
import com.commerceflow.catalog.application.CatalogDtos.PageView;
import com.commerceflow.catalog.application.CatalogDtos.ReviewInput;
import com.commerceflow.catalog.application.CatalogDtos.ReviewView;
import com.commerceflow.catalog.application.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/categories")
    public List<CategoryView> categories() { return service.categories(); }

    @GetMapping("/products")
    public PageView<ProductView> products(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category, @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return service.search(q, category, minPrice, maxPrice, sort, page, size, false);
    }

    @GetMapping("/categories/{slug}/products")
    public PageView<ProductView> categoryProducts(@PathVariable String slug,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return service.search("", slug, null, null, "newest", page, size, false);
    }

    @GetMapping("/products/{slug}")
    public ProductView product(@PathVariable String slug) { return service.detail(slug); }

    @GetMapping("/products/{productId}/reviews")
    public PageView<ReviewView> reviews(@PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.reviews(productId, page, size);
    }

    @PostMapping("/products/{productId}/reviews")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView review(@PathVariable UUID productId, @Valid @RequestBody ReviewInput input,
                              @AuthenticationPrincipal Jwt jwt) {
        return service.submitReview(productId, UUID.fromString(jwt.getSubject()), input);
    }
}

package com.commerceflow.catalog.web;

import com.commerceflow.catalog.application.CatalogDtos.CategoryInput;
import com.commerceflow.catalog.application.CatalogDtos.CategoryView;
import com.commerceflow.catalog.application.CatalogDtos.ProductInput;
import com.commerceflow.catalog.application.CatalogDtos.ProductView;
import com.commerceflow.catalog.application.CatalogDtos.PageView;
import com.commerceflow.catalog.application.CatalogDtos.ModerationInput;
import com.commerceflow.catalog.application.CatalogDtos.ReviewView;
import com.commerceflow.catalog.application.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AdminCatalogController {
    private final CatalogService service;
    public AdminCatalogController(CatalogService service) { this.service = service; }

    @GetMapping("/admin/products")
    public PageView<ProductView> products(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        return service.search(q, "", null, null, "newest", page, size, true);
    }
    @GetMapping("/admin/products/{id}")
    public ProductView product(@PathVariable UUID id) { return service.adminDetail(id); }

    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody ProductInput input, @AuthenticationPrincipal Jwt jwt) {
        return service.saveProduct(null, input, UUID.fromString(jwt.getSubject()));
    }
    @PutMapping("/admin/products/{id}")
    public ProductView update(@PathVariable UUID id, @Valid @RequestBody ProductInput input,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.saveProduct(id, input, UUID.fromString(jwt.getSubject()));
    }
    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@Valid @RequestBody CategoryInput input, @AuthenticationPrincipal Jwt jwt) {
        return service.saveCategory(null, input, UUID.fromString(jwt.getSubject()));
    }
    @PutMapping("/admin/categories/{id}")
    public CategoryView updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryInput input,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.saveCategory(id, input, UUID.fromString(jwt.getSubject()));
    }
    @GetMapping("/admin/reviews")
    public PageView<ReviewView> pending(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "24") int size) {
        return service.pendingReviews(page, size);
    }
    @PatchMapping("/admin/reviews/{id}")
    public ReviewView moderate(@PathVariable UUID id, @Valid @RequestBody ModerationInput input,
                                @AuthenticationPrincipal Jwt jwt) {
        return service.moderate(id, input, UUID.fromString(jwt.getSubject()));
    }
}

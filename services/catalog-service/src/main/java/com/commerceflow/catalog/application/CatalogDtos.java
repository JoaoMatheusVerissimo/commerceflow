package com.commerceflow.catalog.application;

import com.commerceflow.catalog.domain.Category;
import com.commerceflow.catalog.domain.Product;
import com.commerceflow.catalog.domain.Review;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CatalogDtos {
    private CatalogDtos() { }
    public record CategoryInput(@NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @Min(0) Long version) { }
    public record CategoryView(UUID id, String name, String slug, Long version) {
        public static CategoryView from(Category c) {
            return new CategoryView(c.getId(), c.getName(), c.getSlug(), c.getVersion());
        }
    }
    public record VariantInput(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z0-9]+(?:-[A-Z0-9]+)*") String sku,
            @NotBlank @Size(max = 60) String color, @NotBlank @Size(max = 40) String size,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal promotionalPrice) { }
    public record ImageInput(@NotBlank @Size(max = 1000) String url,
                             @NotBlank @Size(max = 160) String alt) { }
    public record ProductInput(@NotNull UUID categoryId, @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @NotBlank @Size(max = 5000) String description,
            @NotNull @Pattern(regexp = "DRAFT|ACTIVE|ARCHIVED") String status,
            @NotNull @Size(min = 1, max = 50) List<@Valid VariantInput> variants,
            @NotNull @Size(min = 1, max = 12) List<@Valid ImageInput> images, @Min(0) Long version) { }
    public record VariantView(String sku, String color, String size, String price, String promotionalPrice,
                              String effectivePrice) { }
    public record ImageView(String url, String alt) { }
    public record ProductView(UUID id, CategoryView category, String name, String slug, String description,
            String status, String minPrice, String currency, Long version,
            List<VariantView> variants, List<ImageView> images) {
        public static ProductView from(Product p) {
            return new ProductView(p.getId(), CategoryView.from(p.getCategory()), p.getName(), p.getSlug(),
                    p.getDescription(), p.getStatus(), money(p.getMinPrice()), "BRL", p.getVersion(),
                    p.getVariants().stream().map(v -> new VariantView(v.getSku(), v.getColor(), v.getSize(),
                            money(v.getPrice()), money(v.getPromotionalPrice()), money(v.effectivePrice()))).toList(),
                    p.getImages().stream().map(i -> new ImageView(i.getUrl(), i.getAlt())).toList());
        }
    }
    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) { }
    public record ReviewInput(@Min(1) @Max(5) int rating, @NotBlank @Size(max = 2000) String comment) { }
    public record ModerationInput(@NotNull @Pattern(regexp = "APPROVED|REJECTED") String status,
                                   @NotNull @Min(0) Long version) { }
    public record ReviewView(UUID id, UUID productId, int rating, String comment, String status,
                             Instant createdAt, Long version) {
        public static ReviewView from(Review r) {
            return new ReviewView(r.getId(), r.getProductId(), r.getRating(), r.getComment(), r.getStatus(),
                    r.getCreatedAt(), r.getVersion());
        }
    }
    private static String money(BigDecimal value) { return value == null ? null : value.setScale(2).toPlainString(); }
}

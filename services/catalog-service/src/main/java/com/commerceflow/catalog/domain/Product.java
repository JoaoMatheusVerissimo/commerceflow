package com.commerceflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "category_id") private Category category;
    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(nullable = false, length = 5000) private String description;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "min_price", nullable = false, precision = 12, scale = 2) private BigDecimal minPrice;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private Long version;
    @ElementCollection @CollectionTable(name = "product_variants", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position") @BatchSize(size = 50)
    private List<Variant> variants = new ArrayList<>();
    @ElementCollection @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position") @BatchSize(size = 50)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() { }
    public Product(Instant now) { id = UUID.randomUUID(); createdAt = now; }
    public void update(Category category, String name, String slug, String description, String status,
                       List<Variant> variants, List<ProductImage> images, Instant now) {
        if (variants.isEmpty() || images.isEmpty()) {
            throw new IllegalArgumentException("Product requires variants and images");
        }
        this.category = category; this.name = name; this.slug = slug;
        this.description = description; this.status = status; this.updatedAt = now;
        this.variants.clear(); this.variants.addAll(variants);
        this.images.clear(); this.images.addAll(images);
        minPrice = variants.stream().map(Variant::effectivePrice).min(BigDecimal::compareTo).orElseThrow();
    }
    public UUID getId() { return id; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public BigDecimal getMinPrice() { return minPrice; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public List<Variant> getVariants() { return List.copyOf(variants); }
    public List<ProductImage> getImages() { return List.copyOf(images); }
}

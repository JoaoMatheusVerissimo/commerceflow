package com.commerceflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "author_id", nullable = false) private UUID authorId;
    @Column(nullable = false) private int rating;
    @Column(nullable = false, length = 2000) private String comment;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Version private Long version;
    protected Review() { }
    public Review(UUID productId, UUID authorId, int rating, String comment, Instant now) {
        id = UUID.randomUUID(); this.productId = productId; this.authorId = authorId;
        this.rating = rating; this.comment = comment; this.createdAt = now; status = "PENDING";
    }
    public void moderate(String status) { this.status = status; }
    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
}

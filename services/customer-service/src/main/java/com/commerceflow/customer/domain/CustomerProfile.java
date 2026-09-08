package com.commerceflow.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected CustomerProfile() { }

    public CustomerProfile(UUID userId, String name, String email, Instant now) {
        id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.email = email;
        createdAt = now;
        updatedAt = now;
    }

    public void update(String name, String email, Instant now) {
        this.name = name;
        this.email = email;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

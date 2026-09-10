package com.commerceflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, unique = true, length = 100) private String slug;
    @Version private Long version;

    protected Category() { }
    public Category(String name, String slug) { id = UUID.randomUUID(); update(name, slug); }
    public void update(String name, String slug) { this.name = name; this.slug = slug; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Long getVersion() { return version; }
}

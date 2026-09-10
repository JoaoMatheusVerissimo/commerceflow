package com.commerceflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductImage {
    @Column(nullable = false, length = 1000) private String url;
    @Column(nullable = false, length = 160) private String alt;
    protected ProductImage() { }
    public ProductImage(String url, String alt) { this.url = url; this.alt = alt; }
    public String getUrl() { return url; }
    public String getAlt() { return alt; }
}

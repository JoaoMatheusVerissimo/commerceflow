package com.commerceflow.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class Variant {
    @Column(nullable = false, length = 64) private String sku;
    @Column(nullable = false, length = 60) private String color;
    @Column(nullable = false, length = 40) private String size;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "promotional_price", precision = 12, scale = 2) private BigDecimal promotionalPrice;

    protected Variant() { }
    public Variant(String sku, String color, String size, BigDecimal price, BigDecimal promotionalPrice) {
        if (price == null || price.signum() <= 0 || price.scale() > 2 || price.precision() > 12
                || (promotionalPrice != null && (promotionalPrice.signum() <= 0
                || promotionalPrice.compareTo(price) >= 0 || promotionalPrice.scale() > 2))) {
            throw new IllegalArgumentException("Invalid variant price or promotion");
        }
        this.sku = sku; this.color = color; this.size = size;
        this.price = price; this.promotionalPrice = promotionalPrice;
    }
    public String getSku() { return sku; }
    public String getColor() { return color; }
    public String getSize() { return size; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPromotionalPrice() { return promotionalPrice; }
    public BigDecimal effectivePrice() { return promotionalPrice == null ? price : promotionalPrice; }
}

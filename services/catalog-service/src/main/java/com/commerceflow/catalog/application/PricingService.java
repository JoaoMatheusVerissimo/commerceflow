package com.commerceflow.catalog.application;

import com.commerceflow.catalog.domain.ProductRepository;
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
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {
    private final ProductRepository products;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public PricingService(ProductRepository products, JdbcTemplate jdbc, Clock clock) {
        this.products = products; this.jdbc = jdbc; this.clock = clock;
    }
    public record Item(@NotNull UUID productId,
            @NotBlank @Pattern(regexp = "[A-Z0-9]+(?:-[A-Z0-9]+)*") @Size(max = 64) String sku,
            @Min(1) @Max(99) int quantity) { }
    public record Request(@NotNull @Size(min = 1, max = 50) List<@Valid Item> items,
            @Pattern(regexp = "[A-Z0-9-]{1,32}") String coupon) { }
    public record Line(UUID productId, String sku, String name, String slug, int quantity,
            String unitPrice, String total) { }
    public record Quote(List<Line> items, String subtotal, String discount, String total,
            String currency, String coupon, Instant quotedAt, Instant expiresAt) { }
    public record Coupon(@NotBlank @Pattern(regexp = "[A-Z0-9-]{1,32}") String code,
            @NotNull @Pattern(regexp = "PERCENT|FIXED") String kind,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal minimum,
            @NotNull Instant startsAt, @NotNull Instant endsAt, boolean active, @Min(0) Long version) { }

    @Transactional(readOnly = true)
    public Quote quote(Request request) {
        var ids = request.items().stream().map(Item::productId).distinct().toList();
        var found = products.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(
                com.commerceflow.catalog.domain.Product::getId, p -> p));
        var seen = new HashSet<String>();
        var lines = new ArrayList<Line>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : request.items()) {
            if (!seen.add(item.sku())) { throw invalid("DUPLICATE_SKU", "Duplicate SKU in quote"); }
            var product = found.get(item.productId());
            if (product == null || !"ACTIVE".equals(product.getStatus())) {
                throw invalid("PRODUCT_UNAVAILABLE", "A product is no longer available; remove it from the cart");
            }
            var variant = product.getVariants().stream().filter(v -> v.getSku().equals(item.sku())).findFirst()
                    .orElseThrow(() -> invalid("SKU_UNAVAILABLE", "A variant is no longer available"));
            var price = variant.effectivePrice();
            var total = price.multiply(BigDecimal.valueOf(item.quantity()));
            subtotal = subtotal.add(total);
            lines.add(new Line(product.getId(), item.sku(), product.getName(), product.getSlug(),
                    item.quantity(), money(price), money(total)));
        }
        var now = clock.instant();
        BigDecimal discount = BigDecimal.ZERO;
        if (request.coupon() != null) {
            var coupon = coupons(request.coupon()).stream().findFirst()
                    .orElseThrow(() -> invalid("COUPON_INVALID", "Coupon does not exist"));
            if (!coupon.active() || now.isBefore(coupon.startsAt()) || !now.isBefore(coupon.endsAt())
                    || subtotal.compareTo(coupon.minimum()) < 0) {
                throw invalid("COUPON_INELIGIBLE", "Coupon inactive, expired or minimum not reached");
            }
            discount = "PERCENT".equals(coupon.kind())
                    ? subtotal.multiply(coupon.amount()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    : coupon.amount();
            discount = discount.min(subtotal);
        }
        return new Quote(List.copyOf(lines), money(subtotal), money(discount), money(subtotal.subtract(discount)),
                "BRL", request.coupon(), now, now.plusSeconds(300));
    }
    public List<Coupon> coupons(String code) {
        return jdbc.query("SELECT * FROM coupons WHERE code = ?", (r, n) -> new Coupon(r.getString("code"),
                r.getString("kind"), r.getBigDecimal("amount"), r.getBigDecimal("minimum"),
                r.getObject("starts_at", OffsetDateTime.class).toInstant(),
                r.getObject("ends_at", OffsetDateTime.class).toInstant(), r.getBoolean("active"),
                r.getLong("version")), code);
    }
    @Transactional
    public Coupon save(Coupon c, UUID actor) {
        if (!c.endsAt().isAfter(c.startsAt()) || ("PERCENT".equals(c.kind())
                && c.amount().compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("Invalid coupon period or percentage");
        }
        if (c.version() == null) {
            jdbc.update("INSERT INTO coupons(code,kind,amount,minimum,starts_at,ends_at,active,version) "
                    + "VALUES (?,?,?,?,?,?,?,0)",
                    c.code(), c.kind(), c.amount(), c.minimum(), c.startsAt().atOffset(ZoneOffset.UTC),
                    c.endsAt().atOffset(ZoneOffset.UTC), c.active());
        } else {
            int changed = jdbc.update("UPDATE coupons SET kind=?,amount=?,minimum=?,starts_at=?,ends_at=?,"
                    + "active=?,version=version+1 WHERE code=? AND version=?",
                    c.kind(), c.amount(), c.minimum(), c.startsAt().atOffset(ZoneOffset.UTC),
                    c.endsAt().atOffset(ZoneOffset.UTC), c.active(), c.code(), c.version());
            if (changed != 1) { throw new CatalogException(409, "COUPON_CONFLICT", "Reload coupon before editing"); }
        }
        jdbc.update("INSERT INTO catalog_audit(id,actor_id,action,resource_id,occurred_at) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), actor, "COUPON_SAVED",
                UUID.nameUUIDFromBytes(c.code().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                clock.instant().atOffset(ZoneOffset.UTC));
        return coupons(c.code()).getFirst();
    }
    private static CatalogException invalid(String code, String message) {
        return new CatalogException(422, code, message);
    }
    private static String money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}

package com.commerceflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CartService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;
    public CartService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Item(@NotNull UUID productId,
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Z0-9]+(?:-[A-Z0-9]+)*") String sku,
            @Min(1) @Max(99) int quantity) { }
    public record Change(@Min(0) long version, @NotNull @Size(max = 50) List<@Valid Item> items,
            @Pattern(regexp = "[A-Z0-9-]{1,32}") String coupon) { }
    public record Cart(long version, List<Item> items, String coupon) { }
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Cart get(UUID owner) {
        var carts = jdbc.query("SELECT version,coupon FROM carts WHERE user_id=?",
                (r, n) -> new Cart(r.getLong("version"), List.of(), r.getString("coupon")), owner);
        if (carts.isEmpty()) { return new Cart(0, List.of(), null); }
        var items = jdbc.query("SELECT product_id,sku,quantity FROM cart_items WHERE user_id=? ORDER BY sku",
                (r, n) -> new Item(r.getObject("product_id", UUID.class), r.getString("sku"),
                        r.getInt("quantity")), owner);
        return new Cart(carts.getFirst().version(), items, carts.getFirst().coupon());
    }
    @Transactional
    public Cart replace(UUID owner, UUID command, Change change) {
        String hash = hash(change);
        var previous = jdbc.query("SELECT request_hash,response FROM cart_commands WHERE user_id=? AND command_id=?",
                (r, n) -> new String[]{r.getString(1), r.getString(2)}, owner, command);
        if (!previous.isEmpty()) {
            if (!previous.getFirst()[0].equals(hash)) { throw conflict("Idempotency key reused with different data"); }
            return JSON.readValue(previous.getFirst()[1], Cart.class);
        }
        var skus = new HashSet<String>();
        for (var item : change.items()) {
            if (!skus.add(item.sku())) { throw new IllegalArgumentException("Duplicate SKU"); }
        }
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        int changed = jdbc.update("UPDATE carts SET version=version+1,coupon=?,updated_at=? "
                + "WHERE user_id=? AND version=?",
                change.coupon(), now, owner, change.version());
        if (changed == 0) {
            if (change.version() != 0 || jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE user_id=?",
                    Long.class, owner) != 0) { throw conflict("Cart changed; reload before editing"); }
            jdbc.update("INSERT INTO carts(user_id,version,coupon,updated_at) VALUES (?,1,?,?)",
                    owner, change.coupon(), now);
        }
        jdbc.update("DELETE FROM cart_items WHERE user_id=?", owner);
        for (var item : change.items()) {
            jdbc.update("INSERT INTO cart_items(user_id,product_id,sku,quantity) VALUES (?,?,?,?)",
                    owner, item.productId(), item.sku(), item.quantity());
        }
        var result = new Cart(change.version() + 1, change.items(), change.coupon());
        jdbc.update("INSERT INTO cart_commands(user_id,command_id,request_hash,response,created_at) VALUES (?,?,?,?,?)",
                owner, command, hash, JSON.writeValueAsString(result), now);
        return result;
    }
    private static String hash(Change change) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(JSON.writeValueAsString(change).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

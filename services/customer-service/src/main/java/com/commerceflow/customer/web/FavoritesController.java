package com.commerceflow.customer.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class FavoritesController {
    private final JdbcTemplate jdbc;
    public FavoritesController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Favorite(UUID productId) { }
    public record Page(List<Favorite> items, int page, long totalElements, long totalPages) { }
    @GetMapping("/customers/me/favorites")
    @Transactional(readOnly = true)
    public Page list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        if (page < 0 || page > 10000) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page"); }
        var owner = UUID.fromString(jwt.getSubject());
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_id=?", Long.class, owner);
        var items = jdbc.query("SELECT product_id FROM favorites WHERE user_id=? ORDER BY created_at DESC,product_id "
                + "LIMIT 20 OFFSET ?", (r, n) -> new Favorite(r.getObject(1, UUID.class)), owner, page * 20);
        return new Page(items, page, total, (total + 19) / 20);
    }
    @PutMapping("/customers/me/favorites/{productId}")
    @Transactional
    public Map<String, Boolean> add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        var owner = UUID.fromString(jwt.getSubject());
        // Lock the profile to serialize additions and enforce the per-user limit, including concurrent requests.
        if (jdbc.queryForList("SELECT user_id FROM customer_profiles WHERE user_id=? FOR UPDATE", owner).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_id=? AND product_id=?",
                Long.class, owner, productId) == 0) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_id=?", Long.class, owner) >= 200) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Favorite limit reached");
            }
            jdbc.update("INSERT INTO favorites(user_id,product_id,created_at) VALUES (?,?,?)",
                    owner, productId, OffsetDateTime.now(ZoneOffset.UTC));
        }
        return Map.of("saved", true);
    }
    @DeleteMapping("/customers/me/favorites/{productId}")
    @Transactional
    public Map<String, Boolean> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        jdbc.update("DELETE FROM favorites WHERE user_id=? AND product_id=?",
                UUID.fromString(jwt.getSubject()), productId);
        return Map.of("saved", false);
    }
}

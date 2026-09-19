package com.commerceflow.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InventoryService {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public InventoryService(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    public record ReservationItem(@NotBlank @Size(max = 64)
            @Pattern(regexp = "[A-Z0-9]+(?:-[A-Z0-9]+)*") String sku,
            @Min(1) @Max(99) int quantity) { }
    public record ReserveRequest(@NotNull UUID orderId,
            @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid ReservationItem> items) { }
    public record Reservation(UUID orderId, UUID reservationId, String status,
            OffsetDateTime expiresAt, List<ReservationItem> items) { }
    public record Stock(String sku, int physical, int reserved, int available, int minimum,
            long version, boolean lowStock) { }
    public record Movement(@NotNull @Pattern(regexp = "IN|OUT|ADJUSTMENT") String kind,
            @NotNull Integer quantity, @NotBlank @Size(max = 240) String reason) { }
    public record Minimum(@Min(0) int minimum, @Min(0) long version) { }

    @Transactional
    public Reservation reserve(UUID owner, ReserveRequest request) {
        var sorted = request.items().stream().sorted(Comparator.comparing(ReservationItem::sku)).toList();
        if (sorted.stream().map(ReservationItem::sku).distinct().count() != sorted.size()) {
            throw new InventoryException(400, "DUPLICATE_SKU", "Reservation contains duplicate SKU");
        }
        String hash = hash(sorted);
        var previous = jdbc.query("SELECT owner_id,request_hash,status,expires_at FROM reservations WHERE order_id=?",
                (r, n) -> new Object[]{r.getObject(1, UUID.class), r.getString(2), r.getString(3),
                        r.getObject(4, OffsetDateTime.class)}, request.orderId());
        if (!previous.isEmpty()) {
            if (!owner.equals(previous.getFirst()[0]) || !hash.equals(previous.getFirst()[1])) {
                throw new InventoryException(409, "RESERVATION_CONFLICT",
                        "Order reservation was already requested with different data");
            }
            return view(request.orderId());
        }
        var locked = new ArrayList<Stock>();
        for (var item : sorted) {
            var found = jdbc.query("SELECT sku,physical,reserved,minimum,version FROM stock_items "
                            + "WHERE sku=? FOR UPDATE",
                    (r, n) -> stock(r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getLong(5)),
                    item.sku());
            if (found.isEmpty() || found.getFirst().available() < item.quantity()) {
                throw new InventoryException(422, "INSUFFICIENT_STOCK", "Insufficient stock for " + item.sku());
            }
            locked.add(found.getFirst());
        }
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var expires = now.plusMinutes(30);
        jdbc.update("INSERT INTO reservations(order_id,owner_id,request_hash,status,expires_at,created_at,updated_at) "
                + "VALUES (?,?,?,'ACTIVE',?,?,?)", request.orderId(), owner, hash, expires, now, now);
        for (int index = 0; index < sorted.size(); index++) {
            var item = sorted.get(index); var before = locked.get(index);
            int changed = jdbc.update("UPDATE stock_items SET reserved=reserved+?,version=version+1,updated_at=? "
                    + "WHERE sku=? AND physical-reserved>=?", item.quantity(), now, item.sku(), item.quantity());
            if (changed != 1) {
                throw new InventoryException(409, "STOCK_CONFLICT", "Stock changed; retry reservation");
            }
            jdbc.update("INSERT INTO reservation_items(order_id,sku,quantity) VALUES (?,?,?)",
                    request.orderId(), item.sku(), item.quantity());
            movement(item.sku(), "RESERVATION", item.quantity(), before.physical(), before.physical(),
                    before.reserved(), before.reserved() + item.quantity(), owner, request.orderId(),
                    "Order inventory reservation", now);
        }
        return new Reservation(request.orderId(), request.orderId(), "ACTIVE", expires, sorted);
    }

    @Transactional(readOnly = true)
    public List<Stock> list() {
        return jdbc.query("SELECT sku,physical,reserved,minimum,version FROM stock_items ORDER BY sku",
                (r, n) -> stock(r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getLong(5)));
    }
    @Transactional(readOnly = true)
    public Stock get(String sku) {
        return jdbc.query("SELECT sku,physical,reserved,minimum,version FROM stock_items WHERE sku=?",
                (r, n) -> stock(r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getLong(5)), sku)
                .stream().findFirst().orElseThrow(() ->
                        new InventoryException(404, "STOCK_NOT_FOUND", "SKU not found"));
    }
    @Transactional
    public Stock move(String sku, Movement input, UUID actor) {
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if ("IN".equals(input.kind())
                && jdbc.queryForObject("SELECT COUNT(*) FROM stock_items WHERE sku=?", Long.class, sku) == 0) {
            jdbc.update("INSERT INTO stock_items(sku,physical,reserved,minimum,version,updated_at) "
                    + "VALUES (?,?,0,0,0,?)", sku, 0, now);
        }
        var before = lock(sku);
        int target = "ADJUSTMENT".equals(input.kind()) ? input.quantity()
                : before.physical() + ("IN".equals(input.kind()) ? input.quantity() : -input.quantity());
        if (input.quantity() < 0 || target < before.reserved()) {
            throw new InventoryException(422, "INVALID_STOCK_MOVEMENT",
                    "Movement cannot make physical stock negative or lower than reserved stock");
        }
        jdbc.update("UPDATE stock_items SET physical=?,version=version+1,updated_at=? WHERE sku=?",
                target, now, sku);
        movement(sku, input.kind(), input.quantity(), before.physical(), target, before.reserved(),
                before.reserved(), actor, null, input.reason(), now);
        return get(sku);
    }
    @Transactional
    public Stock minimum(String sku, Minimum input) {
        int changed = jdbc.update("UPDATE stock_items SET minimum=?,version=version+1,updated_at=? "
                        + "WHERE sku=? AND version=?",
                input.minimum(), OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), sku, input.version());
        if (changed != 1) { throw new InventoryException(409, "STOCK_CONFLICT", "Reload stock before editing"); }
        return get(sku);
    }
    private Reservation view(UUID orderId) {
        var row = jdbc.query("SELECT status,expires_at FROM reservations WHERE order_id=?",
                (r, n) -> new Object[]{r.getString(1), r.getObject(2, OffsetDateTime.class)}, orderId)
                .getFirst();
        var items = jdbc.query("SELECT sku,quantity FROM reservation_items WHERE order_id=? ORDER BY sku",
                (r, n) -> new ReservationItem(r.getString(1), r.getInt(2)), orderId);
        return new Reservation(orderId, orderId, String.valueOf(row[0]), (OffsetDateTime) row[1], items);
    }
    private Stock lock(String sku) {
        return jdbc.query("SELECT sku,physical,reserved,minimum,version FROM stock_items WHERE sku=? FOR UPDATE",
                (r, n) -> stock(r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getLong(5)), sku)
                .stream().findFirst().orElseThrow(() ->
                        new InventoryException(404, "STOCK_NOT_FOUND", "SKU not found"));
    }
    private static Stock stock(String sku, int physical, int reserved, int minimum, long version) {
        int available = physical - reserved;
        return new Stock(sku, physical, reserved, available, minimum, version, available <= minimum);
    }
    private void movement(String sku, String kind, int quantity, int physicalBefore, int physicalAfter,
                          int reservedBefore, int reservedAfter, UUID actor, UUID reference, String reason,
                          OffsetDateTime now) {
        jdbc.update("INSERT INTO stock_movements(id,sku,kind,quantity,physical_before,physical_after,"
                        + "reserved_before,reserved_after,actor_id,reference_id,reason,occurred_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), sku, kind, quantity,
                physicalBefore, physicalAfter, reservedBefore, reservedAfter, actor, reference, reason, now);
    }
    private static String hash(List<ReservationItem> items) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(JSON.writeValueAsBytes(items)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

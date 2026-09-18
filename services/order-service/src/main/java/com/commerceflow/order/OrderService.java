package com.commerceflow.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;
    private final CartService carts;
    private final PricingClient pricing;
    private final InventoryClient inventory;
    private final Clock clock;
    private final TransactionTemplate transactions;
    public OrderService(JdbcTemplate jdbc, CartService carts, PricingClient pricing, InventoryClient inventory,
                        Clock clock, org.springframework.transaction.PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.carts = carts; this.pricing = pricing; this.inventory = inventory;
        this.clock = clock; this.transactions = new TransactionTemplate(manager);
    }
    public record Checkout(@NotNull @Min(0) Long cartVersion) { }
    public record Item(UUID productId, String sku, String name, String slug, int quantity,
                       String unitPrice, String total) { }
    public record Order(UUID id, UUID customerId, String status, List<Item> items, String subtotal,
                        String discount, String shipping, String total, String currency, String coupon,
                        UUID reservationId, String failureCode, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt, long version) { }
    public record Page(List<Order> items, int page, int size, long totalElements, int totalPages) { }

    public Order checkout(UUID customer, UUID command, Checkout input, String token, String correlation) {
        String hash = hash(input);
        var existing = command(customer, command, hash);
        Order order;
        if (existing != null) {
            order = existing;
        } else {
            var cart = carts.get(customer);
            if (cart.version() != input.cartVersion()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart changed; reload checkout");
            }
            var quote = pricing.quote(cart, token, correlation);
            order = transactions.execute(status -> create(customer, command, hash, quote, correlation));
        }
        if (!"CREATED".equals(order.status())) { return order; }
        try {
            var request = new InventoryClient.Request(order.id(), order.items().stream()
                    .map(i -> new InventoryClient.Item(i.sku(), i.quantity())).toList());
            var reservation = inventory.reserve(request, token, correlation);
            return transactions.execute(status -> reserved(order.id(), reservation.reservationId(), customer));
        } catch (InventoryClient.InventoryRejectedException ex) {
            return transactions.execute(status -> cancelled(order.id(), "INSUFFICIENT_STOCK"));
        } catch (InventoryClient.InventoryUnavailableException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Order created but inventory is temporarily unavailable; retry with the same Idempotency-Key");
        }
    }
    public Page history(UUID customer, int page, int size) { return page(customer, false, page, size); }
    public Page admin(int page, int size) { return page(null, true, page, size); }
    public Order get(UUID id, UUID requester, boolean staff) {
        var order = find(id);
        if (!staff && !order.customerId().equals(requester)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }
    private Order create(UUID customer, UUID command, String hash, PricingClient.Quote quote,
                         String correlation) {
        UUID id = UUID.randomUUID(); var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        UUID correlationId;
        try { correlationId = UUID.fromString(correlation); } catch (IllegalArgumentException ex) {
            correlationId = UUID.randomUUID();
        }
        jdbc.update("INSERT INTO orders(id,customer_id,status,subtotal,discount,shipping,total,currency,coupon,"
                        + "correlation_id,created_at,updated_at,version) VALUES (?,?, 'CREATED',?,?,0,?,?,?,?,?,?,0)",
                id, customer, decimal(quote.subtotal()), decimal(quote.discount()), decimal(quote.total()),
                quote.currency(), quote.coupon(), correlationId, now, now);
        for (int position = 0; position < quote.items().size(); position++) {
            var item = quote.items().get(position);
            jdbc.update("INSERT INTO order_items(order_id,position,product_id,sku,name,slug,quantity,unit_price,"
                            + "line_total) VALUES (?,?,?,?,?,?,?,?,?)", id, position, item.productId(), item.sku(),
                    item.name(), item.slug(), item.quantity(), decimal(item.unitPrice()), decimal(item.total()));
        }
        jdbc.update("INSERT INTO checkout_commands(customer_id,command_id,request_hash,order_id,created_at) "
                + "VALUES (?,?,?,?,?)", customer, command, hash, id, now);
        return find(id);
    }
    private Order reserved(UUID id, UUID reservation, UUID customer) {
        var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbc.update("UPDATE orders SET status='PAYMENT_PENDING',reservation_id=?,failure_code=NULL,"
                + "updated_at=?,version=version+1 WHERE id=? AND status='CREATED'", reservation, now, id);
        jdbc.update("DELETE FROM cart_items WHERE user_id=?", customer);
        jdbc.update("UPDATE carts SET coupon=NULL,version=version+1,updated_at=? WHERE user_id=?", now, customer);
        return find(id);
    }
    private Order cancelled(UUID id, String failure) {
        jdbc.update("UPDATE orders SET status='CANCELLED',failure_code=?,updated_at=?,version=version+1 "
                + "WHERE id=? AND status='CREATED'", failure,
                OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC), id);
        return find(id);
    }
    private Order command(UUID customer, UUID command, String hash) {
        var rows = jdbc.query("SELECT request_hash,order_id FROM checkout_commands "
                        + "WHERE customer_id=? AND command_id=?",
                (r, n) -> new Object[]{r.getString(1), r.getObject(2, UUID.class)}, customer, command);
        if (rows.isEmpty()) { return null; }
        if (!hash.equals(rows.getFirst()[0])) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key reused with a different checkout request");
        }
        return find((UUID) rows.getFirst()[1]);
    }
    private Page page(UUID customer, boolean staff, int page, int size) {
        if (page < 0 || size < 1 || size > 50) { throw new IllegalArgumentException("Invalid page"); }
        String where = staff ? "" : " WHERE customer_id=?";
        Object[] args = staff ? new Object[]{} : new Object[]{customer};
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders" + where, Long.class, args);
        var ids = staff
                ? jdbc.query("SELECT id FROM orders ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                        (r, n) -> r.getObject(1, UUID.class), size, page * size)
                : jdbc.query("SELECT id FROM orders WHERE customer_id=? "
                                + "ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",
                        (r, n) -> r.getObject(1, UUID.class), customer, size, page * size);
        return new Page(ids.stream().map(this::find).toList(), page, size, count,
                count == 0 ? 0 : (int) ((count + size - 1) / size));
    }
    private Order find(UUID id) {
        var orders = jdbc.query("SELECT * FROM orders WHERE id=?", (r, n) -> new Order(r.getObject("id", UUID.class),
                r.getObject("customer_id", UUID.class), r.getString("status"), List.of(),
                money(r.getBigDecimal("subtotal")),
                money(r.getBigDecimal("discount")), money(r.getBigDecimal("shipping")), money(r.getBigDecimal("total")),
                r.getString("currency"), r.getString("coupon"), r.getObject("reservation_id", UUID.class),
                r.getString("failure_code"), r.getObject("created_at", OffsetDateTime.class),
                r.getObject("updated_at", OffsetDateTime.class), r.getLong("version")), id);
        if (orders.isEmpty()) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"); }
        var base = orders.getFirst();
        var items = jdbc.query("SELECT * FROM order_items WHERE order_id=? ORDER BY position", (r, n) ->
                new Item(r.getObject("product_id", UUID.class), r.getString("sku"), r.getString("name"),
                        r.getString("slug"), r.getInt("quantity"), money(r.getBigDecimal("unit_price")),
                        money(r.getBigDecimal("line_total"))), id);
        return new Order(base.id(), base.customerId(), base.status(), items, base.subtotal(), base.discount(),
                base.shipping(), base.total(), base.currency(), base.coupon(), base.reservationId(),
                base.failureCode(), base.createdAt(), base.updatedAt(), base.version());
    }
    private static BigDecimal decimal(String value) { return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP); }
    private static String money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String hash(Checkout input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(input.cartVersion()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}

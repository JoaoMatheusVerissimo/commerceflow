package com.commerceflow.inventory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:inventory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="})
@AutoConfigureMockMvc
class InventoryIntegrationTest {
    @Autowired InventoryService inventory;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    UUID owner;
    @BeforeEach void setup() {
        jdbc.update("DELETE FROM stock_movements"); jdbc.update("DELETE FROM reservation_items");
        jdbc.update("DELETE FROM reservations"); jdbc.update("DELETE FROM stock_items");
        jdbc.update("INSERT INTO stock_items(sku,physical,reserved,minimum,version,updated_at) VALUES (?,?,?,?,0,?)",
                "LAST-ONE", 1, 0, 0, OffsetDateTime.now(ZoneOffset.UTC));
        owner = UUID.randomUUID();
    }
    @Test void reservationIsIdempotentAndNeverAcceptsDifferentPayload() {
        var order = UUID.randomUUID();
        var request = new InventoryService.ReserveRequest(order,
                List.of(new InventoryService.ReservationItem("LAST-ONE", 1)));
        assertThat(inventory.reserve(owner, request).status()).isEqualTo("ACTIVE");
        assertThat(inventory.reserve(owner, request).reservationId()).isEqualTo(order);
        assertThat(inventory.get("LAST-ONE").available()).isZero();
        var changed = new InventoryService.ReserveRequest(order,
                List.of(new InventoryService.ReservationItem("LAST-ONE", 2)));
        assertThatThrownBy(() -> inventory.reserve(owner, changed))
                .isInstanceOf(InventoryException.class).hasMessageContaining("different data");
    }
    @Test void twoConcurrentOrdersCannotReserveTheLastUnit() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(2); var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<String> reserve = () -> {
                ready.countDown(); start.await();
                try {
                    inventory.reserve(UUID.randomUUID(), new InventoryService.ReserveRequest(UUID.randomUUID(),
                            List.of(new InventoryService.ReservationItem("LAST-ONE", 1))));
                    return "reserved";
                } catch (InventoryException ex) { return ex.getCode(); }
            };
            var first = executor.submit(reserve); var second = executor.submit(reserve);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("reserved", "INSUFFICIENT_STOCK");
        }
        assertThat(inventory.get("LAST-ONE").reserved()).isEqualTo(1);
    }
    @Test void rejectsInsufficientStockWithoutPartialReservation() {
        var request = new InventoryService.ReserveRequest(UUID.randomUUID(),
                List.of(new InventoryService.ReservationItem("LAST-ONE", 2)));
        assertThatThrownBy(() -> inventory.reserve(owner, request))
                .isInstanceOf(InventoryException.class).hasMessageContaining("Insufficient");
        assertThat(inventory.get("LAST-ONE").reserved()).isZero();
    }
    @Test void enforcesPermissionsAndSupportsAuditedMovement() throws Exception {
        mvc.perform(get("/availability/LAST-ONE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(1));
        mvc.perform(get("/admin/inventory")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/inventory").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/inventory/LAST-ONE/movements")
                .with(jwt().jwt(j -> j.subject(owner.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))
                .contentType("application/json").content("{\"kind\":\"IN\",\"quantity\":2,\"reason\":\"Restock\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.physical").value(3));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements", Long.class)).isEqualTo(1);
    }
}

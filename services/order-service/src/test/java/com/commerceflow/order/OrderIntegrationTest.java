package com.commerceflow.order;

import java.time.Instant;
import java.time.OffsetDateTime;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:orders;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="})
@AutoConfigureMockMvc
class OrderIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CartService carts;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean PricingClient pricing;
    @MockitoBean InventoryClient inventory;
    UUID owner;
    @BeforeEach void setup() {
        jdbc.update("DELETE FROM checkout_commands"); jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders"); jdbc.update("DELETE FROM cart_commands");
        jdbc.update("DELETE FROM cart_items"); jdbc.update("DELETE FROM carts");
        owner = UUID.randomUUID();
        var product = UUID.randomUUID();
        carts.replace(owner, UUID.randomUUID(), new CartService.Change(0L,
                List.of(new CartService.Item(product, "SKU-P", 2)), "SAVE10"));
        when(pricing.quote(any(), anyString(), anyString())).thenReturn(new PricingClient.Quote(
                List.of(new PricingClient.Line(product, "SKU-P", "Bota", "bota", 2, "50.00", "100.00")),
                "100.00", "10.00", "90.00", "BRL", "SAVE10", Instant.now(), Instant.now().plusSeconds(300)));
        when(inventory.reserve(any(), anyString(), anyString())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, InventoryClient.Request.class);
            return new InventoryClient.Reservation(request.orderId(), request.orderId(), "ACTIVE",
                    OffsetDateTime.now().plusMinutes(30), request.items());
        });
    }
    @Test void createsReservedOrderClearsCartAndReplaysIdempotently() throws Exception {
        var key = UUID.randomUUID();
        String response = mvc.perform(customer(post("/checkout"), owner).header("Idempotency-Key", key)
                .contentType("application/json").content("{\"cartVersion\":1}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.total").value("90.00")).andReturn().getResponse().getContentAsString();
        String id = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response).get("id").asString();
        mvc.perform(customer(post("/checkout"), owner).header("Idempotency-Key", key)
                .contentType("application/json").content("{\"cartVersion\":1}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(customer(get("/orders"), owner)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(customer(get("/orders/" + id), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Bota"));
        mvc.perform(customer(get("/orders/" + id), UUID.randomUUID())).andExpect(status().isNotFound());
        org.assertj.core.api.Assertions.assertThat(carts.get(owner).items()).isEmpty();
    }
    @Test void recordsCancellationWhenStockIsInsufficient() throws Exception {
        when(inventory.reserve(any(), anyString(), anyString()))
                .thenThrow(new InventoryClient.InventoryRejectedException());
        mvc.perform(customer(post("/checkout"), owner).header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json").content("{\"cartVersion\":1}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.failureCode").value("INSUFFICIENT_STOCK"));
        org.assertj.core.api.Assertions.assertThat(carts.get(owner).items()).hasSize(1);
    }
    @Test void preservesCreatedOrderForSameKeyRetryAfterTransientFailure() throws Exception {
        var key = UUID.randomUUID();
        when(inventory.reserve(any(), anyString(), anyString()))
                .thenThrow(new InventoryClient.InventoryUnavailableException());
        mvc.perform(customer(post("/checkout"), owner).header("Idempotency-Key", key)
                .contentType("application/json").content("{\"cartVersion\":1}"))
                .andExpect(status().isServiceUnavailable());
        when(inventory.reserve(any(), anyString(), anyString())).thenAnswer(invocation -> {
            var request = invocation.getArgument(0, InventoryClient.Request.class);
            return new InventoryClient.Reservation(request.orderId(), request.orderId(), "ACTIVE",
                    OffsetDateTime.now().plusMinutes(30), request.items());
        });
        mvc.perform(customer(post("/checkout"), owner).header("Idempotency-Key", key)
                .contentType("application/json").content("{\"cartVersion\":1}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class))
                .isEqualTo(1);
    }
    @Test void enforcesCustomerOwnershipAndStaffReadPermissions() throws Exception {
        mvc.perform(get("/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/orders").with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/orders").with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))).andExpect(status().isOk());
    }
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder customer(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, UUID user) {
        return request.with(jwt().jwt(j -> j.subject(user.toString()).claim("roles", List.of("CUSTOMER")))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}

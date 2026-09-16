package com.commerceflow.order;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:cart;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="})
@AutoConfigureMockMvc
class CartIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @Test
    void persistsItemsAndCouponAndReplaysWithoutDuplicating() throws Exception {
        var user = UUID.randomUUID();
        var key = UUID.randomUUID();
        var body = body(0, 2);
        mvc.perform(customer(put("/cart"), user).header("Idempotency-Key", key)
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(customer(put("/cart"), user).header("Idempotency-Key", key)
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(customer(get("/cart"), user)).andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.coupon").value("DEMO10"));
        mvc.perform(customer(put("/cart"), user).header("Idempotency-Key", key)
                .contentType("application/json").content(body(0, 3))).andExpect(status().isConflict());
        mvc.perform(customer(put("/cart"), user).header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json").content(body(0, 3))).andExpect(status().isConflict());
        mvc.perform(customer(put("/cart"), user).header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json").content("{\"version\":1,\"items\":[],\"coupon\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test
    void isolatesOwnersAndDeniesOtherRoles() throws Exception {
        var owner = UUID.randomUUID();
        mvc.perform(customer(put("/cart"), owner).header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json").content(body(0, 1))).andExpect(status().isOk());
        mvc.perform(customer(get("/cart"), UUID.randomUUID())).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cart").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isForbidden());
    }
    @Test
    void validatesQuantitiesKeysAndEmptyQuote() throws Exception {
        var owner = UUID.randomUUID();
        mvc.perform(customer(put("/cart"), owner).contentType("application/json").content(body(0, 1)))
                .andExpect(status().isBadRequest());
        for (int quantity : new int[]{0, -1, 100}) {
            mvc.perform(customer(put("/cart"), owner).header("Idempotency-Key", UUID.randomUUID())
                    .contentType("application/json").content(body(0, quantity))).andExpect(status().isBadRequest());
        }
        mvc.perform(customer(post("/cart/quote"), owner)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/cart']").exists());
    }
    private static String body(int version, int quantity) {
        return "{\"version\":" + version + ",\"coupon\":\"DEMO10\",\"items\":[{\"productId\":"
                + "\"10000000-0000-0000-0000-000000000001\",\"sku\":\"BOOT-P\",\"quantity\":" + quantity + "}]}";
    }
    private static MockHttpServletRequestBuilder customer(MockHttpServletRequestBuilder request, UUID user) {
        return request.with(jwt().jwt(j -> j.subject(user.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}

package com.commerceflow.customer;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:favorites;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="})
@AutoConfigureMockMvc
class FavoritesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JwtDecoder decoder;
    @Test
    void persistsIdempotentlyIsolatesOwnersAndRemoves() throws Exception {
        var owner = UUID.randomUUID();
        var product = UUID.randomUUID();
        jdbc.update("INSERT INTO customer_profiles(id,user_id,name,email,created_at,updated_at,version) "
                + "VALUES (?,?, 'Demo','demo@example.test',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",
                UUID.randomUUID(), owner);
        var path = "/customers/me/favorites/" + product;
        mvc.perform(customer(put(path), owner)).andExpect(status().isOk());
        mvc.perform(customer(put(path), owner)).andExpect(status().isOk());
        mvc.perform(customer(get("/customers/me/favorites"), owner))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(customer(get("/customers/me/favorites"), UUID.randomUUID()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(customer(delete(path), UUID.randomUUID())).andExpect(status().isOk());
        mvc.perform(customer(get("/customers/me/favorites"), owner))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(customer(delete(path), owner)).andExpect(status().isOk());
        mvc.perform(customer(delete(path), owner)).andExpect(status().isOk());
        mvc.perform(customer(get("/customers/me/favorites"), owner))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test
    void rejectsAnonymousOtherRolesAndInvalidPagination() throws Exception {
        mvc.perform(get("/customers/me/favorites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/customers/me/favorites").with(jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_SELLER")))).andExpect(status().isForbidden());
        mvc.perform(customer(get("/customers/me/favorites?page=-1"), UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }
    private static MockHttpServletRequestBuilder customer(MockHttpServletRequestBuilder request, UUID owner) {
        return request.with(jwt().jwt(j -> j.subject(owner.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}

package com.commerceflow.customer.web;

import com.commerceflow.customer.domain.CustomerProfileRepository;
import com.commerceflow.customer.config.SecurityConfig;
import com.commerceflow.customer.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, ApplicationConfig.class})
class CustomerControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private CustomerProfileRepository profiles;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void serviceScopeCanProvisionAnIdempotentProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profiles.findByUserId(userId)).thenReturn(Optional.empty());
        mvc.perform(put("/internal/customers/{userId}", userId)
                        .with(jwt().jwt(token -> token.claim("scope", "customer:provision")))
                        .contentType("application/json")
                        .content("{\"name\":\"Ana Silva\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isNoContent());
        verify(profiles).save(any());
    }
}

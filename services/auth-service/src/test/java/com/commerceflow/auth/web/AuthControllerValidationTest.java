package com.commerceflow.auth.web;

import com.commerceflow.auth.application.AuthApplicationService;
import com.commerceflow.auth.application.TokenService;
import com.commerceflow.auth.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerValidationTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AuthApplicationService authService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void rejectsAWeakRegistrationPassword() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
    }
}

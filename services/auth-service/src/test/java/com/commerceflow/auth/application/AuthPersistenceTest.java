package com.commerceflow.auth.application;

import com.commerceflow.auth.domain.UserAccountRepository;
import com.commerceflow.auth.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class AuthPersistenceTest {
    @Autowired private MockMvc mvc;
    @Autowired private AuthApplicationService auth;
    @Autowired private TokenService tokens;
    @Autowired private UserAccountRepository users;
    @Autowired private JwtDecoder decoder;
    @MockitoBean private CustomerProfileClient customer;

    @Test
    void realHttpRegistrationReturnsCookiesAndAuthenticatedIdentity() throws Exception {
        var response = mvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"name\":\"HTTP User\",\"email\":\"http@example.test\","
                                + "\"password\":\"StrongPassword1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getHeaders("Set-Cookie")).anySatisfy(cookie -> {
            assertThat(cookie).contains("commerceflow_csrf=", "Path=/;");
            assertThat(cookie).doesNotContain("HttpOnly");
        });
        String access = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response.getContentAsString()).get("accessToken").asString();
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("http@example.test"));
        mvc.perform(get("/auth/admin/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/register']").exists());
    }

    @Test
    void registrationLoginRotationAndReusePersistAcrossTransactions() {
        var registered = auth.register("Ana Silva", "persist@example.test", "StrongPassword1");
        var user = users.findByEmail("persist@example.test").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getPendingProfileName()).isNull();
        assertThat(user.getPasswordHash()).startsWith("$argon2");
        assertThat(decoder.decode(registered.accessToken()).getSubject()).isEqualTo(user.getId().toString());
        var loggedIn = auth.login("PERSIST@example.test", "StrongPassword1");
        assertThat(loggedIn.accessToken()).isNotBlank();
        assertThatThrownBy(() -> auth.login("persist@example.test", "wrong"))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> tokens.rotate(registered.refreshToken(), "wrong-csrf"))
                .isInstanceOf(AuthException.class);
        var rotated = tokens.rotate(registered.refreshToken(), registered.csrfToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(registered.refreshToken());
        assertThatThrownBy(() -> tokens.rotate(registered.refreshToken(), registered.csrfToken()))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> tokens.rotate(rotated.refreshToken(), rotated.csrfToken()))
                .isInstanceOf(AuthException.class);
        tokens.revokeAll(user.getId());
        assertThatThrownBy(() -> tokens.rotate(loggedIn.refreshToken(), loggedIn.csrfToken()))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void failedProvisioningLeavesARecoverableIdentity() {
        doThrow(new AuthException("PROFILE_PROVISIONING_UNAVAILABLE", "Unavailable",
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)).when(customer).provision(any(), any(), any());
        assertThatThrownBy(() -> auth.register("Retry User", "retry@example.test", "StrongPassword1"))
                .isInstanceOf(AuthException.class);
        var pending = users.findByEmail("retry@example.test").orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(UserStatus.PENDING_PROFILE);
        doNothing().when(customer).provision(any(), any(), any());
        auth.login("retry@example.test", "StrongPassword1");
        assertThat(users.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}

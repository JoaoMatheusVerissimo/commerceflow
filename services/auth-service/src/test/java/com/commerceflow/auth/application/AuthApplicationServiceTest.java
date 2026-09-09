package com.commerceflow.auth.application;

import com.commerceflow.auth.domain.UserAccountRepository;
import com.commerceflow.auth.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {
    private UserAccountRepository users;
    private PasswordEncoder passwordEncoder;
    private CustomerProfileClient customerClient;
    private TokenService tokenService;
    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        users = mock(UserAccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        customerClient = mock(CustomerProfileClient.class);
        tokenService = mock(TokenService.class);
        service = new AuthApplicationService(users, passwordEncoder, customerClient, tokenService,
                Clock.fixed(Instant.parse("2026-09-09T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void registersIdentityAndProvisionsProfileAcrossTheCorrectBoundary() {
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPassword1")).thenReturn("argon-hash");
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var expected = new TokenService.AuthTokens("access", 900, "refresh", "csrf", 2592000);
        when(tokenService.create(any())).thenReturn(expected);

        var result = service.register("Ana Silva", " ANA@example.com ", "StrongPassword1");

        assertThat(result).isEqualTo(expected);
        var user = org.mockito.ArgumentCaptor.forClass(com.commerceflow.auth.domain.UserAccount.class);
        verify(tokenService).create(user.capture());
        verify(customerClient).provision(user.getValue().getId(), "Ana Silva", "ana@example.com");
        assertThat(user.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}

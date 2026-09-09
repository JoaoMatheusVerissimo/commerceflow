package com.commerceflow.auth.application;

import com.commerceflow.auth.domain.RefreshSession;
import com.commerceflow.auth.domain.RefreshSessionRepository;
import com.commerceflow.auth.domain.UserAccountRepository;
import com.commerceflow.auth.security.JwtService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {
    @Test
    void revokesTheTokenFamilyWhenARefreshTokenIsReused() {
        var repository = mock(RefreshSessionRepository.class);
        var now = Instant.parse("2026-09-09T00:00:00Z");
        var family = UUID.randomUUID();
        var reused = new RefreshSession(UUID.randomUUID(), family, "ignored", "csrf", now, now.plusSeconds(60));
        var sibling = new RefreshSession(UUID.randomUUID(), family, "sibling", "csrf", now, now.plusSeconds(60));
        reused.markUsed(now);
        when(repository.findByTokenHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.of(reused));
        when(repository.findAllByFamilyId(family)).thenReturn(List.of(reused, sibling));
        var service = new TokenService(repository, mock(UserAccountRepository.class), mock(JwtService.class),
                Clock.fixed(now, ZoneOffset.UTC), java.time.Duration.ofDays(30));

        assertThatThrownBy(() -> service.rotate("reused-token", "csrf-token"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalid or expired");
        assertThat(sibling.usableAt(now.plusSeconds(1))).isFalse();
    }
}

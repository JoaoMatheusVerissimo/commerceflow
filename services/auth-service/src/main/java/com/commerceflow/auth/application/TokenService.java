package com.commerceflow.auth.application;

import com.commerceflow.auth.domain.RefreshSession;
import com.commerceflow.auth.domain.RefreshSessionRepository;
import com.commerceflow.auth.domain.UserAccount;
import com.commerceflow.auth.domain.UserAccountRepository;
import com.commerceflow.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {
    private final RefreshSessionRepository sessions;
    private final UserAccountRepository users;
    private final JwtService jwtService;
    private final Clock clock;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public TokenService(RefreshSessionRepository sessions, UserAccountRepository users, JwtService jwtService,
                        Clock clock, @Value("${commerceflow.security.refresh-token-ttl:P30D}") Duration refreshTtl) {
        this.sessions = sessions;
        this.users = users;
        this.jwtService = jwtService;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public AuthTokens create(UserAccount user) {
        return create(user, UUID.randomUUID());
    }

    @Transactional
    public AuthTokens rotate(String refreshToken, String csrfToken) {
        var now = clock.instant();
        var current = sessions.findByTokenHash(hash(refreshToken)).orElseThrow(TokenService::invalidToken);
        if (current.isUsedOrRevoked()) {
            sessions.findAllByFamilyId(current.getFamilyId()).forEach(session -> session.revoke(now));
            throw invalidToken();
        }
        if (!current.usableAt(now) || !constantTimeEquals(current.getCsrfHash(), hash(csrfToken))) {
            throw invalidToken();
        }
        current.markUsed(now);
        var user = users.findById(current.getUserId()).orElseThrow(TokenService::invalidToken);
        return create(user, current.getFamilyId());
    }

    @Transactional
    public void revoke(String refreshToken, String csrfToken) {
        sessions.findByTokenHash(hash(refreshToken)).ifPresent(session -> {
            if (constantTimeEquals(session.getCsrfHash(), hash(csrfToken))) {
                var now = clock.instant();
                sessions.findAllByFamilyId(session.getFamilyId()).forEach(item -> item.revoke(now));
            }
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        var now = clock.instant();
        sessions.findAllByUserId(userId).forEach(session -> session.revoke(now));
    }

    private AuthTokens create(UserAccount user, UUID familyId) {
        String refresh = randomToken();
        String csrf = randomToken();
        var now = clock.instant();
        sessions.save(new RefreshSession(user.getId(), familyId, hash(refresh), hash(csrf), now, now.plus(refreshTtl)));
        var access = jwtService.issueAccess(user);
        return new AuthTokens(access.value(), access.expiresIn(), refresh, csrf, refreshTtl.toSeconds());
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    private static AuthException invalidToken() {
        return new AuthException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired",
                HttpStatus.UNAUTHORIZED);
    }

    public record AuthTokens(String accessToken, long accessExpiresIn, String refreshToken,
                             String csrfToken, long refreshExpiresIn) { }
}

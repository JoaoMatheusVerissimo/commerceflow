package com.commerceflow.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "csrf_hash", nullable = false, length = 64)
    private String csrfHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshSession() {
    }

    public RefreshSession(UUID userId, UUID familyId, String tokenHash, String csrfHash,
                          Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.csrfHash = csrfHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean usableAt(Instant now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) { usedAt = now; }
    public void revoke(Instant now) { revokedAt = now; }
    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public String getCsrfHash() { return csrfHash; }
    public boolean isUsedOrRevoked() { return usedAt != null || revokedAt != null; }
}

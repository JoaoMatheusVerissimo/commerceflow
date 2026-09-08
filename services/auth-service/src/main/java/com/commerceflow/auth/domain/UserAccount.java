package com.commerceflow.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;
    @Column(name = "pending_profile_name", length = 120)
    private String pendingProfileName;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Set<Role> roles = new HashSet<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Version
    private long version;

    protected UserAccount() {
    }

    public static UserAccount pending(String email, String passwordHash, String name, Instant now) {
        UserAccount user = new UserAccount();
        user.id = UUID.randomUUID();
        user.email = email;
        user.passwordHash = passwordHash;
        user.pendingProfileName = name;
        user.status = UserStatus.PENDING_PROFILE;
        user.roles.add(Role.CUSTOMER);
        user.createdAt = now;
        return user;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
        pendingProfileName = null;
    }

    public void recordLogin(Instant now) {
        lastLoginAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserStatus getStatus() { return status; }
    public String getPendingProfileName() { return pendingProfileName; }
    public Set<Role> getRoles() { return Set.copyOf(roles); }
}

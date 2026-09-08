package com.commerceflow.auth.application;

import com.commerceflow.auth.domain.UserAccount;
import com.commerceflow.auth.domain.UserAccountRepository;
import com.commerceflow.auth.domain.UserStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthApplicationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CustomerProfileClient customerClient;
    private final TokenService tokenService;
    private final Clock clock;

    public AuthApplicationService(UserAccountRepository users, PasswordEncoder passwordEncoder,
                                  CustomerProfileClient customerClient, TokenService tokenService, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.customerClient = customerClient;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public TokenService.AuthTokens register(String name, String email, String password) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        var existing = users.findByEmail(normalized);
        UserAccount user;
        if (existing.isPresent()) {
            user = existing.get();
            boolean cannotResume = user.getStatus() != UserStatus.PENDING_PROFILE
                    || !passwordEncoder.matches(password, user.getPasswordHash());
            if (cannotResume) {
                throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email is already registered", HttpStatus.CONFLICT);
            }
        } else {
            user = UserAccount.pending(normalized, passwordEncoder.encode(password), name.trim(), clock.instant());
            try {
                users.saveAndFlush(user);
            } catch (DataIntegrityViolationException exception) {
                throw new AuthException("EMAIL_ALREADY_REGISTERED", "Email is already registered", HttpStatus.CONFLICT);
            }
        }
        customerClient.provision(user.getId(), user.getPendingProfileName(), user.getEmail());
        user.activate();
        users.save(user);
        return tokenService.create(user);
    }

    public TokenService.AuthTokens login(String email, String password) {
        var user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(AuthApplicationService::invalidCredentials);
        if (!passwordEncoder.matches(password, user.getPasswordHash()) || user.getStatus() == UserStatus.DISABLED) {
            throw invalidCredentials();
        }
        if (user.getStatus() == UserStatus.PENDING_PROFILE) {
            customerClient.provision(user.getId(), user.getPendingProfileName(), user.getEmail());
            user.activate();
        }
        user.recordLogin(clock.instant());
        users.save(user);
        return tokenService.create(user);
    }

    public UserAccount requireUser(UUID id) {
        return users.findById(id).orElseThrow(AuthApplicationService::invalidCredentials);
    }

    private static AuthException invalidCredentials() {
        return new AuthException("INVALID_CREDENTIALS", "Email or password is invalid", HttpStatus.UNAUTHORIZED);
    }
}

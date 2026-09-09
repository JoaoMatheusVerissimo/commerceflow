package com.commerceflow.auth.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final Set<String> allowed;

    AudienceValidator(Set<String> allowed) { this.allowed = allowed; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience().stream().anyMatch(allowed::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
    }
}

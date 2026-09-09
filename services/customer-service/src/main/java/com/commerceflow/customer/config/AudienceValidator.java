package com.commerceflow.customer.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private static final Set<String> ALLOWED = Set.of("commerceflow-api", "customer-service");

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience().stream().anyMatch(ALLOWED::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
    }
}

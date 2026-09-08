package com.commerceflow.auth.security;

import com.commerceflow.auth.domain.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(JwtEncoder encoder, Clock clock,
                      @Value("${commerceflow.security.issuer}") String issuer,
                      @Value("${commerceflow.security.access-token-ttl:PT15M}") Duration accessTtl) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.accessTtl = accessTtl;
    }

    public IssuedAccessToken issueAccess(UserAccount user) {
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).issuedAt(now).expiresAt(now.plus(accessTtl))
                .subject(user.getId().toString()).audience(List.of("commerceflow-api"))
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(Enum::name).sorted().toList()).build();
        return new IssuedAccessToken(encode(claims), accessTtl.toSeconds());
    }

    public String issueCustomerProvisioningToken() {
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).issuedAt(now).expiresAt(now.plusSeconds(60))
                .subject("auth-service").audience(List.of("customer-service"))
                .claim("scope", "customer:provision").build();
        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public record IssuedAccessToken(String value, long expiresIn) { }
}

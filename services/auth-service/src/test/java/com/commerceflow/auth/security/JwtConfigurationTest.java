package com.commerceflow.auth.security;

import com.commerceflow.auth.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationTest {
    @TempDir Path temporary;

    @Test
    void loadsStablePrivateKeyAndOnlyPublishesPublicMaterial() throws Exception {
        var config = new JwtConfiguration();
        var generated = config.rsaKey("");
        var path = temporary.resolve("key.pem");
        Files.writeString(path, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(generated.toRSAPrivateKey().getEncoded())
                + "\n-----END PRIVATE KEY-----\n");
        var first = config.rsaKey(path.toString());
        var second = config.rsaKey(path.toString());
        assertThat(first.getKeyID()).isEqualTo(second.getKeyID());
        assertThat(config.publicJwkSet(first).getKeys()).allMatch(key -> !key.isPrivate());
    }

    @Test
    void rejectsExpiredWrongIssuerAndServiceAudienceTokens() throws Exception {
        var config = new JwtConfiguration();
        var key = config.rsaKey("");
        var encoder = config.jwtEncoder(key);
        var decoder = config.jwtDecoder(key, "https://issuer.example.test");
        var user = UserAccount.pending("a@example.test", "hash", "Ana", Instant.now());
        user.activate();
        var valid = new JwtService(encoder, Clock.systemUTC(), "https://issuer.example.test", Duration.ofMinutes(15));
        assertThat(decoder.decode(valid.issueAccess(user).value()).getSubject()).isEqualTo(user.getId().toString());
        assertThatThrownBy(() -> decoder.decode(valid.issueCustomerProvisioningToken()))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
        var wrongIssuer = new JwtService(encoder, Clock.systemUTC(), "https://other.example.test",
                Duration.ofMinutes(15));
        assertThatThrownBy(() -> decoder.decode(wrongIssuer.issueAccess(user).value()))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
        var expired = new JwtService(encoder, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                "https://issuer.example.test", Duration.ofMinutes(15));
        assertThatThrownBy(() -> decoder.decode(expired.issueAccess(user).value()))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
    }
}

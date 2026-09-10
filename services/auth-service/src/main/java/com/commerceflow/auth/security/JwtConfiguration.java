package com.commerceflow.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.Set;

@Configuration
public class JwtConfiguration {
    @Bean
    RSAKey rsaKey(@Value("${commerceflow.security.private-key-file:}") String privateKeyFile) throws Exception {
        if (!privateKeyFile.isBlank()) {
            String pem = java.nio.file.Files.readString(java.nio.file.Path.of(privateKeyFile))
                    .replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            var factory = java.security.KeyFactory.getInstance("RSA");
            var spec = new java.security.spec.PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(pem));
            var privateKey = (java.security.interfaces.RSAPrivateCrtKey) factory.generatePrivate(spec);
            if (privateKey.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("JWT key must contain a private RSA key of at least 2048 bits");
            }
            var publicKey = (RSAPublicKey) factory.generatePublic(new java.security.spec.RSAPublicKeySpec(
                    privateKey.getModulus(), privateKey.getPublicExponent()));
            var key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
            return new RSAKey.Builder(key).keyID(key.computeThumbprint().toString()).build();
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey, @Value("${commerceflow.security.issuer}") String issuer)
            throws Exception {
        var decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new AudienceValidator(Set.of("commerceflow-api"))));
        return decoder;
    }

    @Bean
    JWKSet publicJwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey.toPublicJWK());
    }
}

package com.commerceflow.auth.web;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/.well-known")
public class JwkSetController {
    private final JWKSet jwkSet;

    public JwkSetController(JWKSet jwkSet) { this.jwkSet = jwkSet; }

    @GetMapping("/jwks.json")
    public Map<String, Object> keys() { return jwkSet.toJSONObject(); }
}

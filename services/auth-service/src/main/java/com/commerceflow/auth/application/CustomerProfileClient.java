package com.commerceflow.auth.application;

import com.commerceflow.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
public class CustomerProfileClient {
    private final RestClient client;
    private final JwtService jwtService;

    public CustomerProfileClient(RestClient.Builder builder, JwtService jwtService,
                                 @Value("${commerceflow.services.customer-url}") String customerUrl) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(3));
        factory.setReadTimeout(java.time.Duration.ofSeconds(5));
        this.client = builder.baseUrl(customerUrl).requestFactory(factory).build();
        this.jwtService = jwtService;
    }

    public void provision(UUID userId, String name, String email) {
        try {
            client.put().uri("/internal/customers/{userId}", userId)
                    .headers(headers -> {
                        headers.setBearerAuth(jwtService.issueCustomerProvisioningToken());
                        var attributes = org.springframework.web.context.request.RequestContextHolder
                                .getRequestAttributes();
                        if (attributes != null) {
                            Object id = attributes.getAttribute("correlationId", 0);
                            if (id != null) {
                                headers.set("X-Correlation-Id", id.toString());
                            }
                        }
                    })
                    .body(Map.of("name", name, "email", email)).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            throw new AuthException("PROFILE_PROVISIONING_UNAVAILABLE",
                    "Customer profile could not be provisioned; registration can be retried",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}

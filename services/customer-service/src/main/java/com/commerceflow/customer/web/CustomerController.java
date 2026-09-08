package com.commerceflow.customer.web;

import com.commerceflow.customer.domain.CustomerProfile;
import com.commerceflow.customer.domain.CustomerProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@RestController
public class CustomerController {
    private final CustomerProfileRepository profiles;
    private final Clock clock;

    public CustomerController(CustomerProfileRepository profiles) {
        this(profiles, Clock.systemUTC());
    }

    CustomerController(CustomerProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    @PutMapping("/internal/customers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void provision(@PathVariable UUID userId, @Valid @RequestBody ProvisionRequest request) {
        var now = clock.instant();
        var profile = profiles.findByUserId(userId)
                .orElseGet(() -> new CustomerProfile(userId, request.name().trim(), request.email().toLowerCase(), now));
        profile.update(request.name().trim(), request.email().toLowerCase(), now);
        profiles.save(profile);
    }

    @GetMapping("/customers/me")
    public CustomerView me(@AuthenticationPrincipal Jwt jwt) {
        return profiles.findByUserId(UUID.fromString(jwt.getSubject())).map(CustomerView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer profile not found"));
    }

    public record ProvisionRequest(@NotBlank @Size(min = 2, max = 120) String name,
                                   @NotBlank @Email @Size(max = 320) String email) { }
    public record CustomerView(UUID id, UUID userId, String name, String email) {
        static CustomerView from(CustomerProfile profile) {
            return new CustomerView(profile.getId(), profile.getUserId(), profile.getName(), profile.getEmail());
        }
    }
}

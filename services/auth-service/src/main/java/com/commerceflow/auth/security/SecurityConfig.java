package com.commerceflow.auth.security;

import com.commerceflow.auth.web.ApiErrors;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");
        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) ->
                                ApiErrors.write(req, res, 401, "UNAUTHORIZED", "Authentication is required"))
                        .accessDeniedHandler((req, res, ex) ->
                                ApiErrors.write(req, res, 403, "FORBIDDEN", "Access denied")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/auth/logout",
                                "/auth/.well-known/jwks.json", "/actuator/health", "/v3/api-docs/**",
                                "/swagger-ui/**").permitAll()
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                        .authenticationEntryPoint((req, res, ex) ->
                                ApiErrors.write(req, res, 401, "UNAUTHORIZED", "Authentication is required"))
                        .accessDeniedHandler((req, res, ex) ->
                                ApiErrors.write(req, res, 403, "FORBIDDEN", "Access denied")));
        return http.build();
    }
}

package com.commerceflow.auth.web;

import com.commerceflow.auth.application.AuthApplicationService;
import com.commerceflow.auth.application.TokenService;
import com.commerceflow.auth.domain.UserAccount;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "commerceflow_refresh";
    private static final String CSRF_COOKIE = "commerceflow_csrf";
    private final AuthApplicationService authService;
    private final TokenService tokenService;
    private final boolean secureCookies;

    public AuthController(AuthApplicationService authService, TokenService tokenService,
                          @Value("${commerceflow.security.secure-cookies:true}") boolean secureCookies) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return response(authService.register(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return response(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
            @RequestHeader("X-CSRF-Token") String csrfToken) {
        return response(tokenService.rotate(cookie(request, REFRESH_COOKIE), csrfToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
            @RequestHeader("X-CSRF-Token") String csrfToken) {
        tokenService.revoke(cookie(request, REFRESH_COOKIE), csrfToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clear(REFRESH_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, clear(CSRF_COOKIE).toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        tokenService.revokeAll(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal Jwt jwt) {
        return UserView.from(authService.requireUser(UUID.fromString(jwt.getSubject())));
    }

    private ResponseEntity<AuthResponse> response(TokenService.AuthTokens tokens) {
        var refresh = ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken()).httpOnly(true).secure(secureCookies)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ofSeconds(tokens.refreshExpiresIn())).build();
        var csrf = ResponseCookie.from(CSRF_COOKIE, tokens.csrfToken()).httpOnly(false).secure(secureCookies)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ofSeconds(tokens.refreshExpiresIn())).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refresh.toString())
                .header(HttpHeaders.SET_COOKIE, csrf.toString())
                .body(new AuthResponse(tokens.accessToken(), "Bearer", tokens.accessExpiresIn()));
    }

    private static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return "";
        }
        return Arrays.stream(request.getCookies()).filter(item -> item.getName().equals(name))
                .map(Cookie::getValue).findFirst().orElse("");
    }

    private ResponseCookie clear(String name) {
        return ResponseCookie.from(name, "").httpOnly(name.equals(REFRESH_COOKIE)).secure(secureCookies)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                    message = "must contain uppercase, lowercase and a number") String password) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record AuthResponse(String accessToken, String tokenType, long expiresIn) { }
    public record UserView(UUID id, String email, String status, java.util.Set<String> roles) {
        static UserView from(UserAccount user) {
            return new UserView(user.getId(), user.getEmail(), user.getStatus().name(),
                    user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()));
        }
    }
}

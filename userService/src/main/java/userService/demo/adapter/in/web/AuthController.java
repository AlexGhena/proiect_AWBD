package userService.demo.adapter.in.web;

import com.nimbusds.jose.jwk.JWKSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import userService.demo.adapter.in.web.dto.auth.CurrentUserResponse;
import userService.demo.adapter.in.web.dto.auth.LoginRequest;
import userService.demo.adapter.in.web.dto.auth.LoginResponse;
import userService.demo.adapter.in.web.dto.auth.RegisterRequest;
import userService.demo.adapter.in.web.dto.user.UserResponse;
import userService.demo.adapter.in.web.mapper.UserWebMapper;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.in.UserUseCase;
import userService.demo.security.AuthenticatedUser;
import userService.demo.security.AuthenticatedUserResolver;
import userService.demo.security.LoginAttemptService;
import userService.demo.security.jwt.AccessToken;
import userService.demo.security.jwt.AccessTokenIssuer;

import java.util.Map;

/**
 * Authentication surface: userService is the only authority that verifies credentials and mints
 * access tokens. The other services merely validate what is issued here.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PersistentTokenBasedRememberMeServices rememberMeServices;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final UserUseCase userUseCase;
    private final UserWebMapper userMapper;
    private final JWKSet jwtPublicJwkSet;
    private final LoginAttemptService loginAttemptService;

    /**
     * Primes the CSRF cookie. Reading the token is what makes the repository write it, so the SPA
     * calls this once before its first state-changing request.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok(Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()));
    }

    /**
     * The created account is disabled and PENDING: it cannot log in until an administrator approves
     * it via {@code POST /api/users/{id}/approve}.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AppUser created = userUseCase.register(AppUser.builder()
                .username(request.username())
                .email(request.email())
                .build(), request.password());
        log.info("Registered new user {} pending admin approval", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(created));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        String usernameKey = "user:" + request.username();
        String ipKey = "ip:" + httpRequest.getRemoteAddr();

        // Checked (not just recorded) so a locked-out caller never reaches the authentication
        // manager at all - this is what actually stops further password guessing.
        if (loginAttemptService.isBlocked(usernameKey) || loginAttemptService.isBlocked(ipKey)) {
            log.warn("Rejected login for username '{}' from {}: too many recent failed attempts",
                    request.username(), httpRequest.getRemoteAddr());
            // Same generic outcome as a bad password, so a locked-out state cannot be used to
            // enumerate accounts either.
            throw new LockedException("Invalid username or password");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            // Same outcome for an unknown username and a wrong password, so the response cannot be
            // used to enumerate accounts.
            log.warn("Failed login attempt for username '{}'", request.username());
            loginAttemptService.recordFailure(usernameKey);
            loginAttemptService.recordFailure(ipKey);
            throw new BadCredentialsException("Invalid username or password");
        }

        loginAttemptService.recordSuccess(usernameKey);
        loginAttemptService.recordSuccess(ipKey);

        if (request.rememberMe()) {
            rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
        }

        AccessToken token = accessTokenIssuer.issue(authentication);
        log.info("User {} logged in (rememberMe={})", token.userId(), request.rememberMe());
        return ResponseEntity.ok(toLoginResponse(token));
    }

    /**
     * Exchanges a still-valid remember-me cookie for a fresh access token after a browser reload.
     *
     * <p>Authenticating through the remember-me filter makes Spring Security rotate the CSRF token,
     * which deletes the old cookie. The {@link CsrfToken} parameter is resolved so the replacement is
     * materialised and written on this same response; without it the client would be left with no
     * CSRF cookie and its next write would be rejected.
     */
    @PostMapping("/token")
    public ResponseEntity<LoginResponse> refreshToken(Authentication authentication, CsrfToken csrfToken) {
        csrfToken.getToken();
        AccessToken token = accessTokenIssuer.issue(authentication);
        return ResponseEntity.ok(toLoginResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        AuthenticatedUser user = authenticatedUserResolver.require(authentication);
        return ResponseEntity.ok(new CurrentUserResponse(user.id(), user.username(), user.roles()));
    }

    /**
     * Revokes the persistent remember-me series and clears its cookie. The access token itself is
     * held in memory by the client and is discarded there.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) {
        rememberMeServices.logout(request, response, authentication);
        log.info("User {} logged out", authentication == null ? "unknown" : authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** Public verification keys, consumed by bankingService and transactionService. */
    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {
        return jwtPublicJwkSet.toJSONObject();
    }

    private LoginResponse toLoginResponse(AccessToken token) {
        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresAt(),
                token.userId(),
                token.username(),
                token.roles());
    }
}

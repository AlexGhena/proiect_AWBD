package bankingService.demo.security;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Resilience-wrapped userService calls, kept in a separate bean from {@link UserServiceClient}
 * because Resilience4j's {@code @CircuitBreaker}/{@code @TimeLimiter} aspects only fire on calls
 * made through the Spring proxy - a self-invocation inside a single class would bypass them.
 *
 * <p>A userService business "no" (any 4xx - e.g. a 404 for an unknown user) is handled inside the
 * async body and returned as a normal negative result, so it never trips the breaker. Only genuine
 * transport failures and time-limiter timeouts escape to the fallbacks, which fail closed (deny) to
 * preserve the security-sensitive contract of the calls they back.
 *
 * <p>{@code @TimeLimiter} requires a {@link CompletableFuture} return, so each call runs on a worker
 * thread. The caller's {@link SecurityContext} is thread-bound, so it is captured and re-established
 * on that thread - otherwise {@link BearerTokenPropagationInterceptor} would find no token and the
 * downstream call would go out unauthenticated.
 */
@Component
@Slf4j
public class UserServiceRemoteCalls {

    static final String INSTANCE = "userService";

    private final RestClient restClient;

    public UserServiceRemoteCalls(@Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = INSTANCE, fallbackMethod = "userExistsFallback")
    @TimeLimiter(name = INSTANCE)
    public CompletableFuture<Boolean> userExists(UUID userId) {
        return supplyWithSecurityContext(() -> {
            try {
                restClient.get()
                        .uri("/api/users/{id}", userId)
                        .retrieve()
                        .toBodilessEntity();
                return true;
            } catch (RestClientResponseException ex) {
                log.debug("userService refused user {} with status {}", userId, ex.getStatusCode());
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> userExistsFallback(UUID userId, Throwable t) {
        log.error("Could not verify user {} with userService: {}", userId, t.getMessage());
        return CompletableFuture.completedFuture(false);
    }

    @CircuitBreaker(name = INSTANCE, fallbackMethod = "cardholderNameFallback")
    @TimeLimiter(name = INSTANCE)
    public CompletableFuture<Optional<String>> cardholderName(UUID userId) {
        return supplyWithSecurityContext(() -> {
            try {
                ProfileNameResponse profile = restClient.get()
                        .uri("/api/users/{id}/profile", userId)
                        .retrieve()
                        .body(ProfileNameResponse.class);
                if (profile == null || profile.firstName() == null || profile.lastName() == null) {
                    return Optional.<String>empty();
                }
                return Optional.of(profile.firstName() + " " + profile.lastName());
            } catch (RestClientResponseException ex) {
                log.debug("userService has no profile for user {} (status {})", userId, ex.getStatusCode());
                return Optional.<String>empty();
            }
        });
    }

    private CompletableFuture<Optional<String>> cardholderNameFallback(UUID userId, Throwable t) {
        log.warn("Could not fetch profile for user {} from userService: {}", userId, t.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @CircuitBreaker(name = INSTANCE, fallbackMethod = "verifyPasswordFallback")
    @TimeLimiter(name = INSTANCE)
    public CompletableFuture<Boolean> verifyPassword(UUID userId, String rawPassword) {
        return supplyWithSecurityContext(() -> {
            try {
                VerifyPasswordResponse result = restClient.post()
                        .uri("/api/users/{id}/verify-password", userId)
                        .body(new VerifyPasswordRequest(rawPassword))
                        .retrieve()
                        .body(VerifyPasswordResponse.class);
                return result != null && result.valid();
            } catch (RestClientResponseException ex) {
                log.debug("userService refused password verification for user {} with status {}",
                        userId, ex.getStatusCode());
                return false;
            }
        });
    }

    private CompletableFuture<Boolean> verifyPasswordFallback(UUID userId, String rawPassword, Throwable t) {
        log.error("Could not verify password for user {} with userService: {}", userId, t.getMessage());
        return CompletableFuture.completedFuture(false);
    }

    private <T> CompletableFuture<T> supplyWithSecurityContext(Supplier<T> body) {
        SecurityContext caller = SecurityContextHolder.getContext();
        return CompletableFuture.supplyAsync(() -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            SecurityContextHolder.setContext(caller);
            try {
                return body.get();
            } finally {
                SecurityContextHolder.setContext(previous);
            }
        });
    }

    private record ProfileNameResponse(String firstName, String lastName) {
    }

    private record VerifyPasswordRequest(String password) {
    }

    private record VerifyPasswordResponse(boolean valid) {
    }
}

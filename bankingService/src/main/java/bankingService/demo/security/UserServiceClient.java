package bankingService.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

/**
 * Confirms a user UUID is a real userService account before bankingService opens an account
 * against it.
 *
 * <p>The caller's own token is forwarded, so userService applies its usual rule: a caller may look
 * up their own user record, or an ADMIN may look up anyone's. That means this only ever succeeds
 * for a UUID the caller was already allowed to open an account for - it is an existence check, not
 * a separate authorization decision.
 */
@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean userExists(UUID userId) {
        if (userId == null) {
            return false;
        }
        try {
            log.debug("Verifying user {} exists via userService", userId);
            restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            log.debug("userService refused user {} with status {}", userId, ex.getStatusCode());
            return false;
        } catch (RuntimeException ex) {
            // Never fail open: if userService cannot be reached, deny account creation rather than
            // trust an unverified UUID.
            log.error("Could not verify user {} with userService: {}", userId, ex.getMessage());
            return false;
        }
    }

    /**
     * Best-effort lookup used to name an auto-issued debit card. Unlike {@link #userExists}, a
     * failure here should never block account/card creation - callers fall back to a placeholder
     * name instead.
     */
    public Optional<String> getCardholderName(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        try {
            ProfileNameResponse profile = restClient.get()
                    .uri("/api/users/{id}/profile", userId)
                    .retrieve()
                    .body(ProfileNameResponse.class);
            if (profile == null || profile.firstName() == null || profile.lastName() == null) {
                return Optional.empty();
            }
            return Optional.of(profile.firstName() + " " + profile.lastName());
        } catch (RestClientResponseException ex) {
            log.debug("userService has no profile for user {} (status {})", userId, ex.getStatusCode());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Could not fetch profile for user {} from userService: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    private record ProfileNameResponse(String firstName, String lastName) {
    }

    /**
     * Confirms the caller still knows their own current password, before a sensitive card action
     * (reveal details/PIN, change PIN). Fails closed: any error talking to userService is treated as
     * "not verified" rather than letting the action through.
     */
    public boolean verifyPassword(UUID userId, String rawPassword) {
        if (userId == null || rawPassword == null) {
            return false;
        }
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
        } catch (RuntimeException ex) {
            log.error("Could not verify password for user {} with userService: {}", userId, ex.getMessage());
            return false;
        }
    }

    private record VerifyPasswordRequest(String password) {
    }

    private record VerifyPasswordResponse(boolean valid) {
    }
}

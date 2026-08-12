package bankingService.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
}

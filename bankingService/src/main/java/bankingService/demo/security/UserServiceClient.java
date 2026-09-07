package bankingService.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Confirms a user UUID is a real userService account before bankingService opens an account
 * against it, and backs the sensitive card actions with a userService password re-check.
 *
 * <p>The caller's own token is forwarded, so userService applies its usual rule: a caller may look
 * up their own user record, or an ADMIN may look up anyone's. That means this only ever succeeds
 * for a UUID the caller was already allowed to open an account for - it is an existence check, not
 * a separate authorization decision.
 *
 * <p>This is a thin synchronous facade; the actual userService hop is made by
 * {@link UserServiceRemoteCalls}, which wraps it in a circuit breaker and a time limiter. Those
 * fail closed, so the futures joined here always complete (never throw) and callers keep the same
 * simple boolean/Optional contract they had before.
 */
@Component
@Slf4j
public class UserServiceClient {

    private final UserServiceRemoteCalls remoteCalls;

    public UserServiceClient(UserServiceRemoteCalls remoteCalls) {
        this.remoteCalls = remoteCalls;
    }

    public boolean userExists(UUID userId) {
        if (userId == null) {
            return false;
        }
        log.debug("Verifying user {} exists via userService", userId);
        return remoteCalls.userExists(userId).join();
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
        return remoteCalls.cardholderName(userId).join();
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
        return remoteCalls.verifyPassword(userId, rawPassword).join();
    }
}

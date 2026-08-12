package userService.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import userService.demo.config.LoginAttemptProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for {@code /api/auth/login}. Callers are tracked both by username and
 * by client IP, so neither repeated guesses against one account nor a spray across many usernames
 * from one source can run unbounded.
 *
 * <p>State lives in this instance only; a multi-replica deployment needs a shared store (e.g. Redis)
 * for the same guarantee across pods.
 */
@Component
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();
    private final LoginAttemptProperties properties;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(LoginAttemptProperties properties) {
        this(properties, Clock.systemUTC());
    }

    LoginAttemptService(LoginAttemptProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attemptsByKey.get(normalize(key));
        return attempt != null && attempt.isLocked(clock.instant());
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        attemptsByKey.computeIfAbsent(normalize(key), k -> new Attempt())
                .recordFailure(now, properties.window(), properties.lockoutDuration(), properties.maxAttempts());
        cleanupIfNeeded(now);
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(normalize(key));
    }

    private void cleanupIfNeeded(Instant now) {
        if (attemptsByKey.size() <= CLEANUP_THRESHOLD) {
            return;
        }
        attemptsByKey.entrySet().removeIf(entry -> entry.getValue().isStale(now, properties.window()));
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    private static final class Attempt {
        private int failureCount;
        private Instant windowStart;
        private Instant lockedUntil;

        synchronized boolean isLocked(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }

        synchronized void recordFailure(Instant now, Duration window, Duration lockoutDuration, int maxAttempts) {
            if (windowStart == null || now.isAfter(windowStart.plus(window))) {
                windowStart = now;
                failureCount = 0;
                lockedUntil = null;
            }
            failureCount++;
            if (failureCount >= maxAttempts) {
                lockedUntil = now.plus(lockoutDuration);
            }
        }

        synchronized boolean isStale(Instant now, Duration window) {
            if (lockedUntil != null && now.isBefore(lockedUntil)) {
                return false;
            }
            return windowStart == null || now.isAfter(windowStart.plus(window));
        }
    }
}

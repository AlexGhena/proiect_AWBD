package userService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Brute-force guard for {@code /api/auth/login}. Failures are counted per username and per client
 * IP within a rolling {@code window}; reaching {@code maxAttempts} locks that key out for
 * {@code lockoutDuration}.
 */
@ConfigurationProperties(prefix = "app.security.login-attempts")
public record LoginAttemptProperties(Integer maxAttempts, Duration window, Duration lockoutDuration) {

    public LoginAttemptProperties {
        if (maxAttempts == null || maxAttempts < 1) {
            maxAttempts = 5;
        }
        if (window == null) {
            window = Duration.ofMinutes(15);
        }
        if (lockoutDuration == null) {
            lockoutDuration = Duration.ofMinutes(15);
        }
    }
}

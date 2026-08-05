package userService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Schema the JDBC authentication queries target. The datasource URL is not schema-qualified, so the
 * queries name the schema explicitly rather than relying on the connection search path.
 */
@ConfigurationProperties(prefix = "app.security.jdbc")
public record JdbcAuthenticationProperties(String schema) {

    public JdbcAuthenticationProperties {
        if (schema == null || schema.isBlank()) {
            schema = "users";
        }
    }
}

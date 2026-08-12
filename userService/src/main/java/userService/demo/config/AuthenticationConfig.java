package userService.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * JDBC-backed authentication. Credentials are read straight from the project schema through
 * Spring Security JDBC; day-to-day user CRUD stays on Spring Data JPA.
 */
@Configuration
@EnableConfigurationProperties({JdbcAuthenticationProperties.class, RememberMeProperties.class, LoginAttemptProperties.class})
public class AuthenticationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsManager(DataSource dataSource,
                                                     JdbcAuthenticationProperties properties) {
        String schema = properties.schema();
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery("""
                SELECT username, password_hash, enabled
                FROM %s.app_users
                WHERE username = ?
                """.formatted(schema));
        manager.setAuthoritiesByUsernameQuery("""
                SELECT u.username, r.name AS authority
                FROM %s.app_users u
                JOIN %s.user_roles ur ON ur.user_id = u.id
                JOIN %s.roles r ON r.id = ur.role_id
                WHERE u.username = ?
                """.formatted(schema, schema, schema));
        return manager;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Keep the timing of "unknown user" and "wrong password" alike so the 401 cannot be used
        // to enumerate usernames.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public RememberMeAuthenticationProvider rememberMeAuthenticationProvider(RememberMeProperties properties) {
        return new RememberMeAuthenticationProvider(properties.key());
    }

    /**
     * Explicit ProviderManager: password login for {@code /api/auth/login} and remember-me for the
     * persistent-cookie flow behind {@code /api/auth/token}.
     */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider,
                                                       RememberMeAuthenticationProvider rememberMeAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider, rememberMeAuthenticationProvider);
    }

    /**
     * Spring Security 7 fixes this repository's SQL to an unqualified {@code persistent_logins}, so the
     * datasource URL carries {@code currentSchema} to put the project schema on the connection search
     * path. The table itself is created by Flyway migration V3.
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(RememberMeProperties properties,
                                                                     UserDetailsService userDetailsService,
                                                                     PersistentTokenRepository tokenRepository) {
        PersistentTokenBasedRememberMeServices services = new PersistentTokenBasedRememberMeServices(
                properties.key(), userDetailsService, tokenRepository);
        services.setCookieName(properties.cookieName());
        services.setTokenValiditySeconds(properties.validitySeconds());
        services.setUseSecureCookie(properties.secureCookie());
        // Login arrives as JSON rather than a form post, so the controller decides when to remember
        // and calls loginSuccess only for those requests.
        services.setAlwaysRemember(true);
        return services;
    }
}

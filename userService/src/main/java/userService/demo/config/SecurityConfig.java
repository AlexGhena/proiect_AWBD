package userService.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import userService.demo.security.ProblemDetailAccessDeniedHandler;
import userService.demo.security.ProblemDetailAuthenticationEntryPoint;

import java.util.List;

/**
 * Filter chain for userService, the single authentication authority.
 *
 * <p>Three mechanisms coexist: password login against the JDBC store, a persistent remember-me
 * cookie that can mint a fresh access token, and Bearer validation for tokens already issued.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    /**
     * Note: the chain deliberately does not pin an {@code authenticationManager}. Doing so would
     * bypass the builder that {@code oauth2ResourceServer} and {@code rememberMe} register their
     * providers into, leaving Bearer tokens unauthenticated. AuthController injects the
     * {@link AuthenticationManager} bean directly for password login instead.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PersistentTokenBasedRememberMeServices rememberMeServices,
                                                   RememberMeProperties rememberMeProperties,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        // Readable by the SPA so it can echo the value back in the X-XSRF-TOKEN header.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Plain handler rather than the XOR-masking default: the SPA sends back exactly
                        // the value it read from the cookie.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Service-to-service endpoints authenticate with a Bearer header only and are
                        // never reached by a browser form, so they carry no CSRF risk.
                        .ignoringRequestMatchers("/internal/**"))
                // Nothing is kept server side: the access token and the remember-me cookie carry the state.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberMeProperties.key())
                        .rememberMeServices(rememberMeServices))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Login and logout are REST endpoints on AuthController, not framework-rendered pages.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    /**
     * The {@code roles} claim already carries the {@code ROLE_} prefix, so the prefix is blanked here
     * to avoid producing {@code ROLE_ROLE_ADMIN}.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        // Explicit origins only: a wildcard is incompatible with credentialed requests.
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

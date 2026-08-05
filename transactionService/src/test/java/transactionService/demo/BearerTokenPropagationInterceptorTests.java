package transactionService.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import transactionService.demo.security.BearerTokenPropagationInterceptor;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The propagation contract: an authenticated inbound request must carry its token onto the outbound
 * call, so the downstream service can validate the same user independently.
 */
class BearerTokenPropagationInterceptorTests {

    private final BearerTokenPropagationInterceptor interceptor = new BearerTokenPropagationInterceptor();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockClientHttpRequest outboundRequest() {
        return new MockClientHttpRequest(HttpMethod.GET, URI.create("http://banking/api/accounts/1"));
    }

    private ClientHttpRequestExecution noOpExecution() {
        return (request, body) -> new MockClientHttpResponse(new byte[0], 200);
    }

    private Jwt jwtWithValue(String tokenValue) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject("12d9ff81-1d75-4c56-acdd-3597207412bc")
                .claim("preferred_username", "elena.dumitrescu")
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }

    @Test
    @DisplayName("The caller's token is copied onto the outbound Authorization header")
    void forwardsBearerTokenFromSecurityContext() throws IOException {
        String tokenValue = "the.original.token";
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwtWithValue(tokenValue),
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        MockClientHttpRequest request = outboundRequest();
        ClientHttpResponse response = interceptor.intercept(request, new byte[0], noOpExecution());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + tokenValue);
        // The token must never end up anywhere it could be logged or cached, such as the query string.
        assertThat(request.getURI().toString()).doesNotContain(tokenValue);
    }

    @Test
    @DisplayName("With no authentication the outbound call carries no Authorization header")
    void addsNothingWhenUnauthenticated() throws IOException {
        MockClientHttpRequest request = outboundRequest();

        interceptor.intercept(request, new byte[0], noOpExecution());

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    @DisplayName("A non-JWT authentication contributes no header")
    void addsNothingForNonJwtAuthentication() throws IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("elena", "secret",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        MockClientHttpRequest request = outboundRequest();
        interceptor.intercept(request, new byte[0], noOpExecution());

        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }
}

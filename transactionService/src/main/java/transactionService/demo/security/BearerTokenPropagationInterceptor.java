package transactionService.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Forwards the caller's Bearer token on outbound service-to-service calls.
 *
 * <p>The downstream service validates the token independently rather than trusting this one, so the
 * user's identity and roles survive the hop without any shared service secret. The token only ever
 * travels in the Authorization header - never in a query parameter, and never in a log line.
 */
@Component
@Slf4j
public class BearerTokenPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        currentTokenValue().ifPresentOrElse(
                token -> request.getHeaders().setBearerAuth(token),
                () -> log.debug("No Bearer token on the security context; outbound call to {} will be "
                        + "unauthenticated", request.getURI().getPath()));
        return execution.execute(request, body);
    }

    private Optional<String> currentTokenValue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        return Optional.of(jwt.getTokenValue());
    }
}

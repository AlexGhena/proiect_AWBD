package userService.demo.adapter.out.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import userService.demo.adapter.out.client.dto.ProvisionAccountRequest;
import userService.demo.adapter.out.client.dto.ProvisionedAccountResponse;
import userService.demo.domain.exception.BankingProvisioningException;
import userService.demo.domain.port.out.BankingServiceClientPort;

import java.util.UUID;

/**
 * Calls bankingService's {@code /internal/accounts/provision} to open the account that comes with
 * an approved registration. Mirrors bankingService's own {@code UserServiceClient}: the caller's own
 * Bearer token is forwarded (see {@link userService.demo.security.BearerTokenPropagationInterceptor}),
 * so bankingService independently re-validates the admin who is approving the request.
 */
@Component
@Slf4j
public class BankingServiceClient implements BankingServiceClientPort {

    private static final String DEFAULT_CURRENCY = "RON";

    private final RestClient restClient;

    public BankingServiceClient(@Qualifier("bankingServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String provisionAccount(UUID userId) {
        try {
            ProvisionedAccountResponse response = restClient.post()
                    .uri("/internal/accounts/provision")
                    .body(new ProvisionAccountRequest(userId, DEFAULT_CURRENCY))
                    .retrieve()
                    .body(ProvisionedAccountResponse.class);
            if (response == null || response.iban() == null) {
                throw new BankingProvisioningException(
                        "bankingService returned no IBAN while provisioning an account for user " + userId, null);
            }
            return response.iban();
        } catch (RestClientException ex) {
            log.error("Could not provision a bank account for user {}: {}", userId, ex.getMessage());
            throw new BankingProvisioningException(
                    "Could not provision a bank account for user " + userId, ex);
        }
    }
}

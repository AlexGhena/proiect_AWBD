package transactionService.demo.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Asks bankingService whether the current caller may see an account.
 *
 * <p>The account itself lives in the other service, so ownership cannot be decided locally. The
 * caller's own token is forwarded and bankingService applies its own rules: a 2xx means the caller
 * is allowed to read that account, a 403 or 404 means they are not. That keeps the authorization
 * decision with the service that owns the data.
 */
@Component
@Slf4j
public class BankingAccountClient {

    private final RestClient restClient;

    public BankingAccountClient(@Qualifier("bankingServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean callerCanAccessAccount(UUID accountId) {
        if (accountId == null) {
            return false;
        }
        try {
            log.debug("Verifying account {} access via bankingService", accountId);
            restClient.get()
                    .uri("/api/accounts/{id}", accountId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            log.debug("bankingService refused account {} with status {}", accountId, ex.getStatusCode());
            return false;
        } catch (RuntimeException ex) {
            // Never fail open: if the ownership check cannot be completed, deny.
            log.error("Could not verify account ownership with bankingService: {}", ex.getMessage());
            return false;
        }
    }

    /** The account ids bankingService considers to belong to the caller, per their own token. */
    public List<UUID> callerAccountIds() {
        try {
            AccountsPageResponse page = restClient.get()
                    .uri("/api/accounts/me?size=100")
                    .retrieve()
                    .body(AccountsPageResponse.class);
            return page == null ? List.of() : page.content().stream().map(AccountRef::id).toList();
        } catch (RestClientResponseException ex) {
            log.debug("bankingService refused /api/accounts/me with status {}", ex.getStatusCode());
            return List.of();
        } catch (RuntimeException ex) {
            log.error("Could not fetch the caller's accounts from bankingService: {}", ex.getMessage());
            return List.of();
        }
    }

    private record AccountsPageResponse(List<AccountRef> content) {
    }

    private record AccountRef(UUID id) {
    }
}

package transactionService.demo.adapter.out.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import transactionService.demo.domain.exception.BankingBusinessException;
import transactionService.demo.domain.exception.BankingServiceUnavailableException;
import transactionService.demo.domain.model.BankingAccountSnapshot;
import transactionService.demo.domain.model.BankingOperationResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Balance operations the transfer Saga performs against bankingService's {@code /internal/**}
 * endpoints. Every mutating call carries an {@code Idempotency-Key} so a retried request never
 * double-applies. {@code @Retry}/{@code @CircuitBreaker} wrap only genuine transport failures - a
 * 4xx from bankingService is a business decision and is translated to {@link BankingBusinessException}
 * before resilience4j ever sees it, so it is never retried and never counted against the breaker
 * (see the {@code resilience4j.*.ignore-exceptions} config for the "bankingService" instance).
 */
@Component
@Slf4j
public class BankingTransferClient {

    private static final String INSTANCE = "bankingService";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public BankingTransferClient(@Qualifier("bankingServiceRestClient") RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "getSnapshotFallback")
    public BankingAccountSnapshot getSnapshot(UUID accountId) {
        return call(() -> restClient.get()
                .uri("/internal/accounts/{id}", accountId)
                .retrieve()
                .body(BankingAccountSnapshot.class));
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "debitFallback")
    public BankingOperationResult debit(UUID accountId, BigDecimal amount, String currency, UUID sagaId, String idempotencyKey) {
        return call(() -> restClient.post()
                .uri("/internal/accounts/{id}/debit", accountId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AmountRequestBody(amount, currency, sagaId))
                .retrieve()
                .body(BankingOperationResult.class));
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "creditFallback")
    public BankingOperationResult credit(UUID accountId, BigDecimal amount, String currency, UUID sagaId, String idempotencyKey) {
        return call(() -> restClient.post()
                .uri("/internal/accounts/{id}/credit", accountId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AmountRequestBody(amount, currency, sagaId))
                .retrieve()
                .body(BankingOperationResult.class));
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "compensateFallback")
    public BankingOperationResult compensate(UUID accountId, String originalIdempotencyKey, UUID sagaId, String idempotencyKey) {
        return call(() -> restClient.post()
                .uri("/internal/accounts/{id}/compensate", accountId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CompensateRequestBody(originalIdempotencyKey, sagaId))
                .retrieve()
                .body(BankingOperationResult.class));
    }

    private BankingAccountSnapshot getSnapshotFallback(UUID accountId, Throwable t) {
        throw translate("fetching a snapshot for account " + accountId, t);
    }

    private BankingOperationResult debitFallback(UUID accountId, BigDecimal amount, String currency, UUID sagaId,
                                                  String idempotencyKey, Throwable t) {
        throw translate("debiting account " + accountId, t);
    }

    private BankingOperationResult creditFallback(UUID accountId, BigDecimal amount, String currency, UUID sagaId,
                                                   String idempotencyKey, Throwable t) {
        throw translate("crediting account " + accountId, t);
    }

    private BankingOperationResult compensateFallback(UUID accountId, String originalIdempotencyKey, UUID sagaId,
                                                        String idempotencyKey, Throwable t) {
        throw translate("compensating a debit on account " + accountId, t);
    }

    /**
     * A 4xx is bankingService's own considered decision - re-thrown as-is regardless of whether
     * resilience4j routed it through the fallback. Anything else (network failure, 5xx, an open
     * circuit) means bankingService itself could not complete the operation.
     */
    private RuntimeException translate(String action, Throwable t) {
        if (t instanceof BankingBusinessException businessException) {
            return businessException;
        }
        log.error("bankingService unavailable while {}: {}", action, t.getMessage());
        return new BankingServiceUnavailableException("bankingService unavailable while " + action, t);
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BankingBusinessException(ex.getStatusCode(), extractDetail(ex));
            }
            throw ex;
        }
    }

    /**
     * bankingService's error body is a {@code ProblemDetail} JSON object; pulling out just its
     * {@code detail} text avoids embedding a whole raw JSON document inside this exception's own
     * (also JSON-serialized) message.
     */
    private String extractDetail(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body.isBlank()) {
            return ex.getMessage();
        }
        try {
            JsonNode detail = objectMapper.readTree(body).get("detail");
            if (detail != null && !detail.isNull()) {
                return detail.asString();
            }
        } catch (RuntimeException parseFailure) {
            log.debug("Could not parse bankingService's error body as JSON: {}", parseFailure.getMessage());
        }
        return body;
    }

    private record AmountRequestBody(BigDecimal amount, String currency, UUID sagaId) {
    }

    private record CompensateRequestBody(String originalIdempotencyKey, UUID sagaId) {
    }
}

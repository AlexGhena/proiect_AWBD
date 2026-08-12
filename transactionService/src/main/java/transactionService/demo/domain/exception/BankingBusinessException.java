package transactionService.demo.domain.exception;

import org.springframework.http.HttpStatusCode;

/**
 * A considered business rejection of a transfer - either a 4xx bankingService itself returned
 * (insufficient funds, an unknown or blocked account, ...), or a rejection the Saga makes locally
 * from account data bankingService returned (a currency mismatch between the transfer and one of
 * the accounts). Passed straight through to the caller with the same status rather than retried
 * or treated as a circuit-breaker failure.
 */
public class BankingBusinessException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public BankingBusinessException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}

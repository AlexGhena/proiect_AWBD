package transactionService.demo.domain.exception;

/** bankingService could not be reached, or retries/the circuit breaker were exhausted. */
public class BankingServiceUnavailableException extends RuntimeException {

    public BankingServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

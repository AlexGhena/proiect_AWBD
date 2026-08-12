package bankingService.demo.domain.exception;

/** Raised when a reused Idempotency-Key is replayed with a different account, operation or amount. */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}

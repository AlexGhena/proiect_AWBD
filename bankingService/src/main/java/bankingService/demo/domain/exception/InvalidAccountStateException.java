package bankingService.demo.domain.exception;

/** Raised when a balance operation targets an account that is not ACTIVE, or a currency mismatch. */
public class InvalidAccountStateException extends RuntimeException {

    public InvalidAccountStateException(String message) {
        super(message);
    }
}

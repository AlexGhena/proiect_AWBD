package bankingService.demo.domain.exception;

/** Raised when an update targets a card in a state that forbids it (e.g. already LOST_STOLEN). */
public class InvalidCardStateException extends RuntimeException {

    public InvalidCardStateException(String message) {
        super(message);
    }
}

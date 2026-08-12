package userService.demo.domain.exception;

/** Thrown when approve/reject is attempted on a user that is not currently PENDING. */
public class InvalidRegistrationStateException extends RuntimeException {

    public InvalidRegistrationStateException(String message) {
        super(message);
    }
}

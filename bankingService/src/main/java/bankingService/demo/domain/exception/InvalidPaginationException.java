package bankingService.demo.domain.exception;

/** A page/size/sort request that fails validation before it ever reaches the repository. */
public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}

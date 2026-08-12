package userService.demo.domain.exception;

/** Thrown when approving a user succeeds locally but bankingService cannot provision the account. */
public class BankingProvisioningException extends RuntimeException {

    public BankingProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}

package userService.demo.domain.port.out;

import java.util.UUID;

/** Provisions the bank account that comes with an approved registration. */
public interface BankingServiceClientPort {

    /**
     * Asks bankingService to open a new account for {@code userId} and returns the IBAN it
     * generated.
     *
     * @throws userService.demo.domain.exception.BankingProvisioningException if bankingService
     *         cannot be reached or refuses the request
     */
    String provisionAccount(UUID userId);
}

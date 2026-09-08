package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AccountUseCase {

    BankAccount createAccount(BankAccount account);

    /**
     * Opens an account with a freshly generated IBAN, for a user whose registration userService just
     * approved. Reached only via {@code /internal/accounts/provision} - any authenticated caller, not
     * gated by resource ownership - since userService already established the caller is entitled to
     * act on this user's behalf.
     */
    BankAccount provisionAccount(UUID userId, String currency);

    BankAccount getAccount(UUID id);

    /**
     * Resolves an account by its IBAN so a caller can look up a transfer destination they do not
     * own (the whole point of paying to someone else's IBAN). Deliberately not ownership-gated;
     * the {@code AccountLookupResponse} it feeds exposes only non-sensitive fields (id, currency,
     * status) - never balance or owner - so this cannot be used to enumerate account details.
     */
    BankAccount resolveByIban(String iban);

    Page<BankAccount> listAccounts(Pageable pageable);

    Page<BankAccount> listMyAccounts(Pageable pageable);

    BankAccount updateAccount(UUID id, BankAccount updates);

    void deleteAccount(UUID id);
}

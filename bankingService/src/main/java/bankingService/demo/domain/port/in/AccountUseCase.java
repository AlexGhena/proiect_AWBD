package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AccountUseCase {

    BankAccount createAccount(BankAccount account);

    BankAccount getAccount(UUID id);

    Page<BankAccount> listAccounts(Pageable pageable);

    BankAccount updateAccount(UUID id, BankAccount updates);

    void deleteAccount(UUID id);
}

package bankingService.demo.domain.port.out;

import bankingService.demo.domain.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {

    BankAccount save(BankAccount account);

    Optional<BankAccount> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsByIban(String iban);

    Page<BankAccount> findAll(Pageable pageable);

    Page<BankAccount> findByUserId(UUID userId, Pageable pageable);

    void deleteById(UUID id);
}

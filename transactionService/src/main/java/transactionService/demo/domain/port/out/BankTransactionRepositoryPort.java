package transactionService.demo.domain.port.out;

import transactionService.demo.domain.model.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepositoryPort {

    BankTransaction save(BankTransaction transaction);

    Optional<BankTransaction> findById(UUID id);

    boolean existsById(UUID id);

    boolean existsBySagaId(UUID sagaId);

    Page<BankTransaction> findAll(Pageable pageable);

    Page<BankTransaction> findByAccountIdIn(List<UUID> accountIds, Pageable pageable);

    List<BankTransaction> findByScheduledTransactionId(UUID scheduledTransactionId);

    void deleteById(UUID id);
}

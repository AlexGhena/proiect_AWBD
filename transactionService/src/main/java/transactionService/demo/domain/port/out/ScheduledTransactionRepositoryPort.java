package transactionService.demo.domain.port.out;

import transactionService.demo.domain.model.ScheduledTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ScheduledTransactionRepositoryPort {

    ScheduledTransaction save(ScheduledTransaction scheduledTransaction);

    Optional<ScheduledTransaction> findById(UUID id);

    boolean existsById(UUID id);

    Page<ScheduledTransaction> findAll(Pageable pageable);

    void deleteById(UUID id);
}

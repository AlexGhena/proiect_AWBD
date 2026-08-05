package transactionService.demo.domain.port.in;

import transactionService.demo.domain.model.ScheduledTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ScheduledTransactionUseCase {

    ScheduledTransaction createScheduledTransaction(ScheduledTransaction scheduledTransaction);

    ScheduledTransaction getScheduledTransaction(UUID id);

    Page<ScheduledTransaction> listScheduledTransactions(Pageable pageable);

    ScheduledTransaction updateScheduledTransaction(UUID id, ScheduledTransaction updates);

    void deleteScheduledTransaction(UUID id);
}

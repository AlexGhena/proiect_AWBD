package transactionService.demo.domain.port.in;

import transactionService.demo.domain.model.BankTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BankTransactionUseCase {

    BankTransaction createTransaction(BankTransaction transaction);

    BankTransaction getTransaction(UUID id);

    Page<BankTransaction> listTransactions(Pageable pageable);

    List<BankTransaction> listTransactionsBySchedule(UUID scheduledTransactionId);

    BankTransaction updateTransaction(UUID id, BankTransaction updates);

    void deleteTransaction(UUID id);
}

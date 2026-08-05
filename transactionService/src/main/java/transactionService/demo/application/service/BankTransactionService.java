package transactionService.demo.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transactionService.demo.domain.exception.DuplicateResourceException;
import transactionService.demo.domain.exception.ResourceNotFoundException;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.model.TransactionStatus;
import transactionService.demo.domain.port.in.BankTransactionUseCase;
import transactionService.demo.domain.port.out.BankTransactionRepositoryPort;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;
import transactionService.demo.domain.port.out.ScheduledTransactionRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BankTransactionService implements BankTransactionUseCase {

    private final BankTransactionRepositoryPort bankTransactionRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ScheduledTransactionRepositoryPort scheduledTransactionRepositoryPort;

    @Override
    public BankTransaction createTransaction(BankTransaction transaction) {
        if (transaction.getCategoryId() != null && !categoryRepositoryPort.existsById(transaction.getCategoryId())) {
            throw new ResourceNotFoundException("Transaction category " + transaction.getCategoryId() + " not found");
        }
        if (transaction.getScheduledTransactionId() != null
                && !scheduledTransactionRepositoryPort.existsById(transaction.getScheduledTransactionId())) {
            throw new ResourceNotFoundException(
                    "Scheduled transaction " + transaction.getScheduledTransactionId() + " not found");
        }
        transaction.setId(null);
        if (transaction.getSagaId() == null) {
            transaction.setSagaId(UUID.randomUUID());
        } else if (bankTransactionRepositoryPort.existsBySagaId(transaction.getSagaId())) {
            throw new DuplicateResourceException(
                    "Bank transaction with saga id " + transaction.getSagaId() + " already exists");
        }
        if (transaction.getStatus() == null) {
            transaction.setStatus(TransactionStatus.PENDING);
        }
        return bankTransactionRepositoryPort.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public BankTransaction getTransaction(UUID id) {
        return bankTransactionRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank transaction " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankTransaction> listTransactions(Pageable pageable) {
        return bankTransactionRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankTransaction> listTransactionsBySchedule(UUID scheduledTransactionId) {
        if (!scheduledTransactionRepositoryPort.existsById(scheduledTransactionId)) {
            throw new ResourceNotFoundException("Scheduled transaction " + scheduledTransactionId + " not found");
        }
        return bankTransactionRepositoryPort.findByScheduledTransactionId(scheduledTransactionId);
    }

    @Override
    public BankTransaction updateTransaction(UUID id, BankTransaction updates) {
        BankTransaction existing = getTransaction(id);
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getFailureReason() != null) {
            existing.setFailureReason(updates.getFailureReason());
        }
        return bankTransactionRepositoryPort.save(existing);
    }

    @Override
    public void deleteTransaction(UUID id) {
        if (!bankTransactionRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Bank transaction " + id + " not found");
        }
        bankTransactionRepositoryPort.deleteById(id);
    }
}

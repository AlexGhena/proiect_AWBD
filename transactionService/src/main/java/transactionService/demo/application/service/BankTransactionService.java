package transactionService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Slf4j
public class BankTransactionService implements BankTransactionUseCase {

    private final BankTransactionRepositoryPort bankTransactionRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ScheduledTransactionRepositoryPort scheduledTransactionRepositoryPort;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @transactionSecurity.ownsEitherAccount(#transaction.sourceAccountId, #transaction.destinationAccountId)")
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
        BankTransaction saved = bankTransactionRepositoryPort.save(transaction);
        log.info("Bank transaction created with id={}, sagaId={}", saved.getId(), saved.getSagaId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @transactionSecurity.ownsTransaction(#id)")
    public BankTransaction getTransaction(UUID id) {
        return bankTransactionRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank transaction " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BankTransaction> listTransactions(Pageable pageable) {
        log.debug("Listing bank transactions with pageable={}", pageable);
        return bankTransactionRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or @transactionSecurity.ownsScheduledTransaction(#scheduledTransactionId)")
    public List<BankTransaction> listTransactionsBySchedule(UUID scheduledTransactionId) {
        if (!scheduledTransactionRepositoryPort.existsById(scheduledTransactionId)) {
            throw new ResourceNotFoundException("Scheduled transaction " + scheduledTransactionId + " not found");
        }
        return bankTransactionRepositoryPort.findByScheduledTransactionId(scheduledTransactionId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @transactionSecurity.ownsTransaction(#id)")
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
        BankTransaction saved = bankTransactionRepositoryPort.save(existing);
        log.info("Bank transaction updated with id={}, status={}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTransaction(UUID id) {
        if (!bankTransactionRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Bank transaction " + id + " not found");
        }
        bankTransactionRepositoryPort.deleteById(id);
        log.info("Bank transaction deleted with id={}", id);
    }
}

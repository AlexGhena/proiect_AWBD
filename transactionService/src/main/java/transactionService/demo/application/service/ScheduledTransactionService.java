package transactionService.demo.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import transactionService.demo.domain.exception.ResourceNotFoundException;
import transactionService.demo.domain.model.ScheduleStatus;
import transactionService.demo.domain.model.ScheduledTransaction;
import transactionService.demo.domain.port.in.ScheduledTransactionUseCase;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;
import transactionService.demo.domain.port.out.ScheduledTransactionRepositoryPort;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduledTransactionService implements ScheduledTransactionUseCase {

    private final ScheduledTransactionRepositoryPort scheduledTransactionRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;

    @Override
    public ScheduledTransaction createScheduledTransaction(ScheduledTransaction scheduledTransaction) {
        if (scheduledTransaction.getCategoryId() != null
                && !categoryRepositoryPort.existsById(scheduledTransaction.getCategoryId())) {
            throw new ResourceNotFoundException(
                    "Transaction category " + scheduledTransaction.getCategoryId() + " not found");
        }
        scheduledTransaction.setId(null);
        if (scheduledTransaction.getStatus() == null) {
            scheduledTransaction.setStatus(ScheduleStatus.ACTIVE);
        }
        return scheduledTransactionRepositoryPort.save(scheduledTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTransaction getScheduledTransaction(UUID id) {
        return scheduledTransactionRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled transaction " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledTransaction> listScheduledTransactions(Pageable pageable) {
        return scheduledTransactionRepositoryPort.findAll(pageable);
    }

    @Override
    public ScheduledTransaction updateScheduledTransaction(UUID id, ScheduledTransaction updates) {
        ScheduledTransaction existing = getScheduledTransaction(id);
        if (updates.getCategoryId() != null) {
            if (!categoryRepositoryPort.existsById(updates.getCategoryId())) {
                throw new ResourceNotFoundException("Transaction category " + updates.getCategoryId() + " not found");
            }
            existing.setCategoryId(updates.getCategoryId());
        }
        if (updates.getAmount() != null) {
            existing.setAmount(updates.getAmount());
        }
        if (updates.getCurrency() != null) {
            existing.setCurrency(updates.getCurrency());
        }
        if (updates.getFrequency() != null) {
            existing.setFrequency(updates.getFrequency());
        }
        if (updates.getNextExecutionDate() != null) {
            existing.setNextExecutionDate(updates.getNextExecutionDate());
        }
        if (updates.getStatus() != null) {
            existing.setStatus(updates.getStatus());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        return scheduledTransactionRepositoryPort.save(existing);
    }

    @Override
    public void deleteScheduledTransaction(UUID id) {
        if (!scheduledTransactionRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Scheduled transaction " + id + " not found");
        }
        scheduledTransactionRepositoryPort.deleteById(id);
    }
}

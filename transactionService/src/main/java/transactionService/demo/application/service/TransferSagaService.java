package transactionService.demo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import transactionService.demo.adapter.out.client.BankingTransferClient;
import transactionService.demo.domain.exception.BankingBusinessException;
import transactionService.demo.domain.exception.BankingServiceUnavailableException;
import transactionService.demo.domain.exception.ResourceNotFoundException;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.model.BankingAccountSnapshot;
import transactionService.demo.domain.model.TransactionStatus;
import transactionService.demo.domain.port.in.TransferUseCase;
import transactionService.demo.domain.port.out.BankTransactionRepositoryPort;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;

import java.util.UUID;

/**
 * The transfer Saga (docs/BACKEND_ARCHITECTURE.md section 9): create PENDING, validate both
 * accounts, debit the source, credit the destination, compensating the debit if the credit fails.
 *
 * <p>Deliberately NOT {@code @Transactional} at the class level: each status transition below is
 * persisted through its own auto-committing repository call (see {@code SimpleJpaRepository}) so
 * that a FAILED/COMPENSATED status written after a downstream failure survives even though the
 * method itself goes on to throw. Wrapping the whole method in one local transaction would roll
 * those writes back along with the exception.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaService implements TransferUseCase {

    private final BankTransactionRepositoryPort bankTransactionRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final BankingTransferClient bankingTransferClient;

    @Override
    @PreAuthorize("hasRole('ADMIN') or @bankingAccountClient.callerCanAccessAccount(#transaction.sourceAccountId)")
    public BankTransaction executeTransfer(BankTransaction transaction) {
        if (transaction.getCategoryId() != null && !categoryRepositoryPort.existsById(transaction.getCategoryId())) {
            throw new ResourceNotFoundException("Transaction category " + transaction.getCategoryId() + " not found");
        }

        UUID sagaId = UUID.randomUUID();
        transaction.setId(null);
        transaction.setSagaId(sagaId);
        transaction.setStatus(TransactionStatus.PENDING);
        BankTransaction saga = bankTransactionRepositoryPort.save(transaction);
        log.info("Transfer saga started: sagaId={}, transactionId={}, source={}, destination={}, amount={} {}",
                sagaId, saga.getId(), saga.getSourceAccountId(), saga.getDestinationAccountId(),
                saga.getAmount(), saga.getCurrency());

        validateAccounts(saga);

        String debitKey = sagaId + ":DEBIT";
        try {
            bankingTransferClient.debit(saga.getSourceAccountId(), saga.getAmount(), saga.getCurrency(), sagaId, debitKey);
            log.info("Saga {}: source account {} debited, idempotencyKey={}", sagaId, saga.getSourceAccountId(), debitKey);
        } catch (RuntimeException ex) {
            markFailed(saga, "Debit failed: " + ex.getMessage());
            throw ex;
        }

        String creditKey = sagaId + ":CREDIT";
        try {
            bankingTransferClient.credit(saga.getDestinationAccountId(), saga.getAmount(), saga.getCurrency(), sagaId, creditKey);
            log.info("Saga {}: destination account {} credited, idempotencyKey={}", sagaId, saga.getDestinationAccountId(), creditKey);
        } catch (RuntimeException ex) {
            // compensate() always returns a non-null exception; throwing it here (rather than
            // inside compensate()) makes the "credit failure never completes the saga" guarantee
            // visible to the compiler instead of relying on compensate() never returning normally.
            throw compensate(saga, debitKey, sagaId, ex);
        }

        saga.setStatus(TransactionStatus.COMPLETED);
        BankTransaction completed = bankTransactionRepositoryPort.save(saga);
        log.info("Saga {} completed: transactionId={}", sagaId, completed.getId());
        return completed;
    }

    /** Fetches both account snapshots before any money moves; rejects a mismatched or inactive account. */
    private void validateAccounts(BankTransaction saga) {
        BankingAccountSnapshot source = bankingTransferClient.getSnapshot(saga.getSourceAccountId());
        BankingAccountSnapshot destination = bankingTransferClient.getSnapshot(saga.getDestinationAccountId());
        if (!source.isActive()) {
            failValidation(saga, "Source account " + source.id() + " is not ACTIVE (status=" + source.status() + ")");
        }
        if (!destination.isActive()) {
            failValidation(saga, "Destination account " + destination.id() + " is not ACTIVE (status=" + destination.status() + ")");
        }
        if (!source.currency().equals(saga.getCurrency()) || !destination.currency().equals(saga.getCurrency())) {
            failValidation(saga, "Account currency does not match the transfer currency " + saga.getCurrency());
        }
    }

    private void failValidation(BankTransaction saga, String reason) {
        markFailed(saga, reason);
        throw new BankingBusinessException(HttpStatus.CONFLICT, reason);
    }

    private void markFailed(BankTransaction saga, String reason) {
        saga.setStatus(TransactionStatus.FAILED);
        saga.setFailureReason(reason);
        bankTransactionRepositoryPort.save(saga);
        log.warn("Saga {} failed: transactionId={}, reason={}", saga.getSagaId(), saga.getId(), reason);
    }

    /**
     * The credit failed after a successful debit, so reverses exactly that debit. Whether or not
     * the reversal itself succeeds, the transfer as a whole did not complete - the caller sees 503
     * either way, per the sequence diagram in docs/BACKEND_ARCHITECTURE.md section 9. A reversal
     * failure is logged at ERROR with every id needed for manual reconciliation.
     */
    private RuntimeException compensate(BankTransaction saga, String debitKey, UUID sagaId, RuntimeException creditFailure) {
        String compensateKey = sagaId + ":COMPENSATE";
        boolean compensated;
        String outcomeMessage;
        try {
            bankingTransferClient.compensate(saga.getSourceAccountId(), debitKey, sagaId, compensateKey);
            compensated = true;
            outcomeMessage = "Credit failed, debit compensated: " + creditFailure.getMessage();
            log.warn("Saga {} compensated: transactionId={}, reason={}", sagaId, saga.getId(), creditFailure.getMessage());
        } catch (RuntimeException compensateFailure) {
            compensated = false;
            outcomeMessage = "Credit failed and compensation ALSO failed - manual reconciliation required: "
                    + compensateFailure.getMessage();
            log.error("Saga {} REQUIRES MANUAL RECONCILIATION: transactionId={}, sourceAccountId={}, debitKey={}, "
                            + "creditFailure={}, compensateFailure={}",
                    sagaId, saga.getId(), saga.getSourceAccountId(), debitKey,
                    creditFailure.getMessage(), compensateFailure.getMessage());
        }
        saga.setStatus(compensated ? TransactionStatus.COMPENSATED : TransactionStatus.FAILED);
        saga.setFailureReason(outcomeMessage);
        bankTransactionRepositoryPort.save(saga);
        return new BankingServiceUnavailableException(outcomeMessage, creditFailure);
    }
}

package bankingService.demo.domain.port.in;

import bankingService.demo.domain.model.AccountOperationResult;
import bankingService.demo.domain.model.AccountSnapshot;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Balance operations used by the transactionService transfer Saga. These are reached only
 * through {@code /internal/**}, so authorization here is "any authenticated caller", not
 * resource ownership: the orchestrator already validated who may initiate the transfer, and the
 * destination account usually belongs to someone other than the caller.
 */
public interface AccountTransferUseCase {

    AccountSnapshot getSnapshot(UUID accountId);

    AccountOperationResult debit(UUID accountId, BigDecimal amount, String currency, UUID sagaId, String idempotencyKey);

    AccountOperationResult credit(UUID accountId, BigDecimal amount, String currency, UUID sagaId, String idempotencyKey);

    /** Reverses the exact amount of the debit recorded under {@code originalIdempotencyKey}. */
    AccountOperationResult compensate(UUID accountId, String originalIdempotencyKey, UUID sagaId, String idempotencyKey);
}

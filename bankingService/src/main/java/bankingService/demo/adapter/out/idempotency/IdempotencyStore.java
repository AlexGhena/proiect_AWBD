package bankingService.demo.adapter.out.idempotency;

import bankingService.demo.domain.exception.IdempotencyConflictException;
import bankingService.demo.domain.model.IdempotentOperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin wrapper around the {@code idempotency_keys} ledger. Callers run inside the same
 * {@code @Transactional} boundary as the balance mutation it records, so the record and the
 * account update commit or roll back together.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    private final IdempotencyKeyJpaRepository repository;

    public Optional<IdempotencyKeyJpaEntity> find(String idempotencyKey) {
        return repository.findById(idempotencyKey);
    }

    /** Fails closed: a replayed key must match the account and operation it was recorded for. */
    public void verifyReplayMatches(IdempotencyKeyJpaEntity existing, UUID accountId,
                                     IdempotentOperationType operation) {
        if (!existing.getAccountId().equals(accountId) || existing.getOperation() != operation) {
            throw new IdempotencyConflictException(
                    "Idempotency key " + existing.getIdempotencyKey()
                            + " was already used for a different account or operation");
        }
    }

    /** As above, plus the caller-supplied amount must match (debit/credit, where the amount is an input). */
    public void verifyReplayMatches(IdempotencyKeyJpaEntity existing, UUID accountId,
                                     IdempotentOperationType operation, BigDecimal amount) {
        verifyReplayMatches(existing, accountId, operation);
        if (existing.getAmount().compareTo(amount) != 0) {
            throw new IdempotencyConflictException(
                    "Idempotency key " + existing.getIdempotencyKey() + " was already used with a different amount");
        }
    }

    public IdempotencyKeyJpaEntity record(String idempotencyKey, UUID accountId, IdempotentOperationType operation,
                                           UUID sagaId, BigDecimal amount, String currency, BigDecimal balanceAfter) {
        IdempotencyKeyJpaEntity entity = IdempotencyKeyJpaEntity.builder()
                .idempotencyKey(idempotencyKey)
                .accountId(accountId)
                .operation(operation)
                .sagaId(sagaId)
                .amount(amount)
                .currency(currency)
                .balanceAfter(balanceAfter)
                .build();
        return repository.saveAndFlush(entity);
    }
}

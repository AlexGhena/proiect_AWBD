package transactionService.demo.adapter.out.persistence.mapper;

import transactionService.demo.adapter.out.persistence.entity.BankTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.ScheduledTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.domain.model.BankTransaction;
import org.springframework.stereotype.Component;

@Component
public class BankTransactionPersistenceMapper {

    public BankTransactionJpaEntity toEntity(BankTransaction domain,
                                              TransactionCategoryJpaEntity categoryRef,
                                              ScheduledTransactionJpaEntity scheduledTransactionRef) {
        if (domain == null) {
            return null;
        }
        return BankTransactionJpaEntity.builder()
                .id(domain.getId())
                .category(categoryRef)
                .scheduledTransaction(scheduledTransactionRef)
                .sourceAccountId(domain.getSourceAccountId())
                .destinationAccountId(domain.getDestinationAccountId())
                .sagaId(domain.getSagaId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .type(domain.getType())
                .status(domain.getStatus())
                .description(domain.getDescription())
                .failureReason(domain.getFailureReason())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public BankTransaction toDomain(BankTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return BankTransaction.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory() == null ? null : entity.getCategory().getId())
                .scheduledTransactionId(entity.getScheduledTransaction() == null
                        ? null : entity.getScheduledTransaction().getId())
                .sourceAccountId(entity.getSourceAccountId())
                .destinationAccountId(entity.getDestinationAccountId())
                .sagaId(entity.getSagaId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .type(entity.getType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

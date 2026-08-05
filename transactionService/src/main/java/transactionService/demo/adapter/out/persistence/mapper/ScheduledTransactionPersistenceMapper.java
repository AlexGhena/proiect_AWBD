package transactionService.demo.adapter.out.persistence.mapper;

import transactionService.demo.adapter.out.persistence.entity.ScheduledTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.domain.model.ScheduledTransaction;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTransactionPersistenceMapper {

    public ScheduledTransactionJpaEntity toEntity(ScheduledTransaction domain, TransactionCategoryJpaEntity categoryRef) {
        if (domain == null) {
            return null;
        }
        return ScheduledTransactionJpaEntity.builder()
                .id(domain.getId())
                .category(categoryRef)
                .sourceAccountId(domain.getSourceAccountId())
                .destinationAccountId(domain.getDestinationAccountId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .frequency(domain.getFrequency())
                .nextExecutionDate(domain.getNextExecutionDate())
                .status(domain.getStatus())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public ScheduledTransaction toDomain(ScheduledTransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ScheduledTransaction.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory() == null ? null : entity.getCategory().getId())
                .sourceAccountId(entity.getSourceAccountId())
                .destinationAccountId(entity.getDestinationAccountId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .frequency(entity.getFrequency())
                .nextExecutionDate(entity.getNextExecutionDate())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

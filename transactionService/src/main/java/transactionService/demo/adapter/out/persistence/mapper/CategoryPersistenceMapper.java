package transactionService.demo.adapter.out.persistence.mapper;

import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.domain.model.TransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public TransactionCategoryJpaEntity toEntity(TransactionCategory domain) {
        if (domain == null) {
            return null;
        }
        return TransactionCategoryJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public TransactionCategory toDomain(TransactionCategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return TransactionCategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

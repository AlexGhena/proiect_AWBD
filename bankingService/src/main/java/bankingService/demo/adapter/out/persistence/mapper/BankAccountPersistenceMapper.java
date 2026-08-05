package bankingService.demo.adapter.out.persistence.mapper;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.domain.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class BankAccountPersistenceMapper {

    public BankAccountJpaEntity toEntity(BankAccount domain) {
        if (domain == null) {
            return null;
        }
        return BankAccountJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .iban(domain.getIban())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .status(domain.getStatus())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public BankAccount toDomain(BankAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return BankAccount.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .iban(entity.getIban())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

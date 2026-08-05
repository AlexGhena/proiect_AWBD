package bankingService.demo.adapter.out.persistence.mapper;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.entity.BankCardJpaEntity;
import bankingService.demo.domain.model.BankCard;
import org.springframework.stereotype.Component;

@Component
public class BankCardPersistenceMapper {

    public BankCardJpaEntity toEntity(BankCard domain, BankAccountJpaEntity accountRef) {
        if (domain == null) {
            return null;
        }
        return BankCardJpaEntity.builder()
                .id(domain.getId())
                .account(accountRef)
                .cardReference(domain.getCardReference())
                .lastFour(domain.getLastFour())
                .cardholderName(domain.getCardholderName())
                .expiryMonth(domain.getExpiryMonth() == null ? null : domain.getExpiryMonth().shortValue())
                .expiryYear(domain.getExpiryYear() == null ? null : domain.getExpiryYear().shortValue())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public BankCard toDomain(BankCardJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return BankCard.builder()
                .id(entity.getId())
                .accountId(entity.getAccount().getId())
                .cardReference(entity.getCardReference())
                .lastFour(entity.getLastFour())
                .cardholderName(entity.getCardholderName())
                .expiryMonth(entity.getExpiryMonth() == null ? null : entity.getExpiryMonth().intValue())
                .expiryYear(entity.getExpiryYear() == null ? null : entity.getExpiryYear().intValue())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

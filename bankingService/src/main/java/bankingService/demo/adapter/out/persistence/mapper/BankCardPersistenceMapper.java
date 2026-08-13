package bankingService.demo.adapter.out.persistence.mapper;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.entity.BankCardJpaEntity;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.security.CardFieldEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BankCardPersistenceMapper {

    private final CardFieldEncryptionService encryptionService;

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
                .cardNumberEncrypted(encryptOrNull(domain.getCardNumber()))
                .cvvEncrypted(encryptOrNull(domain.getCvv()))
                .pinEncrypted(encryptOrNull(domain.getPin()))
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
                .cardNumber(decryptOrNull(entity.getCardNumberEncrypted()))
                .cvv(decryptOrNull(entity.getCvvEncrypted()))
                .pin(decryptOrNull(entity.getPinEncrypted()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String encryptOrNull(String plainText) {
        return plainText == null ? null : encryptionService.encrypt(plainText);
    }

    private String decryptOrNull(String encoded) {
        return encoded == null ? null : encryptionService.decrypt(encoded);
    }
}

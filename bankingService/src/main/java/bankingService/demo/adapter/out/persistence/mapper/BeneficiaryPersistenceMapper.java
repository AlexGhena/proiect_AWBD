package bankingService.demo.adapter.out.persistence.mapper;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.entity.BeneficiaryJpaEntity;
import bankingService.demo.domain.model.Beneficiary;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryPersistenceMapper {

    public BeneficiaryJpaEntity toEntity(Beneficiary domain, BankAccountJpaEntity ownerAccountRef) {
        if (domain == null) {
            return null;
        }
        return BeneficiaryJpaEntity.builder()
                .id(domain.getId())
                .ownerAccount(ownerAccountRef)
                .beneficiaryName(domain.getBeneficiaryName())
                .beneficiaryIban(domain.getBeneficiaryIban())
                .nickname(domain.getNickname())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Beneficiary toDomain(BeneficiaryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Beneficiary.builder()
                .id(entity.getId())
                .ownerAccountId(entity.getOwnerAccount().getId())
                .beneficiaryName(entity.getBeneficiaryName())
                .beneficiaryIban(entity.getBeneficiaryIban())
                .nickname(entity.getNickname())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

package bankingService.demo.adapter.out.persistence.repository;

import bankingService.demo.adapter.out.persistence.entity.BeneficiaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeneficiaryJpaRepository extends JpaRepository<BeneficiaryJpaEntity, UUID> {

    boolean existsByOwnerAccount_IdAndBeneficiaryIban(UUID ownerAccountId, String beneficiaryIban);

    List<BeneficiaryJpaEntity> findByOwnerAccount_Id(UUID ownerAccountId);
}

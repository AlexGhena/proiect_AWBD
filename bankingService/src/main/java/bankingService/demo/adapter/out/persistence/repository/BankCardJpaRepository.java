package bankingService.demo.adapter.out.persistence.repository;

import bankingService.demo.adapter.out.persistence.entity.BankCardJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankCardJpaRepository extends JpaRepository<BankCardJpaEntity, UUID> {

    boolean existsByCardReference(String cardReference);

    List<BankCardJpaEntity> findByAccount_Id(UUID accountId);
}

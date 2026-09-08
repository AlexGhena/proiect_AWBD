package bankingService.demo.adapter.out.persistence.repository;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountJpaRepository extends JpaRepository<BankAccountJpaEntity, UUID> {

    boolean existsByIban(String iban);

    Optional<BankAccountJpaEntity> findByIban(String iban);

    Page<BankAccountJpaEntity> findByUserId(UUID userId, Pageable pageable);
}

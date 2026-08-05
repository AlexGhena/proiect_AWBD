package bankingService.demo.adapter.out.persistence.repository;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BankAccountJpaRepository extends JpaRepository<BankAccountJpaEntity, UUID> {

    boolean existsByIban(String iban);
}

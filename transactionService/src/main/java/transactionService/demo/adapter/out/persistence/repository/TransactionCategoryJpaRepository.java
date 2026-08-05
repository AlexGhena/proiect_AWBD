package transactionService.demo.adapter.out.persistence.repository;

import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionCategoryJpaRepository extends JpaRepository<TransactionCategoryJpaEntity, UUID> {

    boolean existsByName(String name);
}

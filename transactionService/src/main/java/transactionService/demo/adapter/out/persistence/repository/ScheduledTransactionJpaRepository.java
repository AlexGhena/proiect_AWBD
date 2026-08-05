package transactionService.demo.adapter.out.persistence.repository;

import transactionService.demo.adapter.out.persistence.entity.ScheduledTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScheduledTransactionJpaRepository extends JpaRepository<ScheduledTransactionJpaEntity, UUID> {

    boolean existsByCategory_Id(UUID categoryId);
}

package transactionService.demo.adapter.out.persistence.repository;

import transactionService.demo.adapter.out.persistence.entity.BankTransactionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankTransactionJpaRepository extends JpaRepository<BankTransactionJpaEntity, UUID> {

    boolean existsBySagaId(UUID sagaId);

    boolean existsByCategory_Id(UUID categoryId);

    List<BankTransactionJpaEntity> findByScheduledTransaction_Id(UUID scheduledTransactionId);

    Page<BankTransactionJpaEntity> findBySourceAccountIdInOrDestinationAccountIdIn(
            List<UUID> sourceAccountIds, List<UUID> destinationAccountIds, Pageable pageable);
}

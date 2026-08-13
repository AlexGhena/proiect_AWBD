package transactionService.demo.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import transactionService.demo.adapter.out.persistence.entity.BankTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.ScheduledTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.adapter.out.persistence.mapper.BankTransactionPersistenceMapper;
import transactionService.demo.adapter.out.persistence.repository.BankTransactionJpaRepository;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.port.out.BankTransactionRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BankTransactionPersistenceAdapter implements BankTransactionRepositoryPort {

    private final BankTransactionJpaRepository repository;
    private final BankTransactionPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public BankTransaction save(BankTransaction transaction) {
        TransactionCategoryJpaEntity categoryRef = transaction.getCategoryId() == null
                ? null
                : entityManager.getReference(TransactionCategoryJpaEntity.class, transaction.getCategoryId());
        ScheduledTransactionJpaEntity scheduledTransactionRef = transaction.getScheduledTransactionId() == null
                ? null
                : entityManager.getReference(ScheduledTransactionJpaEntity.class, transaction.getScheduledTransactionId());
        BankTransactionJpaEntity entity = mapper.toEntity(transaction, categoryRef, scheduledTransactionRef);
        BankTransactionJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BankTransaction> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsBySagaId(UUID sagaId) {
        return repository.existsBySagaId(sagaId);
    }

    @Override
    public Page<BankTransaction> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<BankTransaction> findByAccountIdIn(List<UUID> accountIds, Pageable pageable) {
        return repository.findBySourceAccountIdInOrDestinationAccountIdIn(accountIds, accountIds, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<BankTransaction> findByScheduledTransactionId(UUID scheduledTransactionId) {
        return repository.findByScheduledTransaction_Id(scheduledTransactionId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

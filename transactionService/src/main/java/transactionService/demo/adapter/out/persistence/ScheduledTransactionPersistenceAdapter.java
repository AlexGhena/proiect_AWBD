package transactionService.demo.adapter.out.persistence;

import transactionService.demo.adapter.out.persistence.entity.ScheduledTransactionJpaEntity;
import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.adapter.out.persistence.mapper.ScheduledTransactionPersistenceMapper;
import transactionService.demo.adapter.out.persistence.repository.ScheduledTransactionJpaRepository;
import transactionService.demo.domain.model.ScheduledTransaction;
import transactionService.demo.domain.port.out.ScheduledTransactionRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ScheduledTransactionPersistenceAdapter implements ScheduledTransactionRepositoryPort {

    private final ScheduledTransactionJpaRepository repository;
    private final ScheduledTransactionPersistenceMapper mapper;
    private final EntityManager entityManager;

    public ScheduledTransactionPersistenceAdapter(ScheduledTransactionJpaRepository repository,
                                                    ScheduledTransactionPersistenceMapper mapper,
                                                    EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public ScheduledTransaction save(ScheduledTransaction scheduledTransaction) {
        TransactionCategoryJpaEntity categoryRef = scheduledTransaction.getCategoryId() == null
                ? null
                : entityManager.getReference(TransactionCategoryJpaEntity.class, scheduledTransaction.getCategoryId());
        ScheduledTransactionJpaEntity entity = mapper.toEntity(scheduledTransaction, categoryRef);
        ScheduledTransactionJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ScheduledTransaction> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public Page<ScheduledTransaction> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

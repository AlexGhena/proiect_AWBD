package transactionService.demo.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import transactionService.demo.adapter.out.persistence.entity.TransactionCategoryJpaEntity;
import transactionService.demo.adapter.out.persistence.mapper.CategoryPersistenceMapper;
import transactionService.demo.adapter.out.persistence.repository.BankTransactionJpaRepository;
import transactionService.demo.adapter.out.persistence.repository.ScheduledTransactionJpaRepository;
import transactionService.demo.adapter.out.persistence.repository.TransactionCategoryJpaRepository;
import transactionService.demo.domain.model.TransactionCategory;
import transactionService.demo.domain.port.out.CategoryRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final TransactionCategoryJpaRepository repository;
    private final BankTransactionJpaRepository bankTransactionJpaRepository;
    private final ScheduledTransactionJpaRepository scheduledTransactionJpaRepository;
    private final CategoryPersistenceMapper mapper;

    @Override
    public TransactionCategory save(TransactionCategory category) {
        TransactionCategoryJpaEntity entity = mapper.toEntity(category);
        TransactionCategoryJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TransactionCategory> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public boolean isReferenced(UUID categoryId) {
        return bankTransactionJpaRepository.existsByCategory_Id(categoryId)
                || scheduledTransactionJpaRepository.existsByCategory_Id(categoryId);
    }

    @Override
    public Page<TransactionCategory> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

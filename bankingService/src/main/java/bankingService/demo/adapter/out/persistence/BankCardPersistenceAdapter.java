package bankingService.demo.adapter.out.persistence;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.entity.BankCardJpaEntity;
import bankingService.demo.adapter.out.persistence.mapper.BankCardPersistenceMapper;
import bankingService.demo.adapter.out.persistence.repository.BankCardJpaRepository;
import bankingService.demo.domain.model.BankCard;
import bankingService.demo.domain.port.out.CardRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BankCardPersistenceAdapter implements CardRepositoryPort {

    private final BankCardJpaRepository repository;
    private final BankCardPersistenceMapper mapper;
    private final EntityManager entityManager;

    public BankCardPersistenceAdapter(BankCardJpaRepository repository,
                                       BankCardPersistenceMapper mapper,
                                       EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public BankCard save(BankCard card) {
        BankAccountJpaEntity accountRef = entityManager.getReference(BankAccountJpaEntity.class, card.getAccountId());
        BankCardJpaEntity entity = mapper.toEntity(card, accountRef);
        BankCardJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BankCard> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByCardReference(String cardReference) {
        return repository.existsByCardReference(cardReference);
    }

    @Override
    public Page<BankCard> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<BankCard> findByAccountId(UUID accountId) {
        return repository.findByAccount_Id(accountId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

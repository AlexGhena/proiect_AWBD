package bankingService.demo.adapter.out.persistence;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.mapper.BankAccountPersistenceMapper;
import bankingService.demo.adapter.out.persistence.repository.BankAccountJpaRepository;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BankAccountPersistenceAdapter implements AccountRepositoryPort {

    private final BankAccountJpaRepository repository;
    private final BankAccountPersistenceMapper mapper;

    @Override
    public BankAccount save(BankAccount account) {
        BankAccountJpaEntity entity = mapper.toEntity(account);
        BankAccountJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BankAccount> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BankAccount> findByIban(String iban) {
        return repository.findByIban(iban).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByIban(String iban) {
        return repository.existsByIban(iban);
    }

    @Override
    public Page<BankAccount> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<BankAccount> findByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

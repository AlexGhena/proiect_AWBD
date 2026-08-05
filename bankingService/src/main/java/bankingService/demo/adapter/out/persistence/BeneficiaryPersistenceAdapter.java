package bankingService.demo.adapter.out.persistence;

import bankingService.demo.adapter.out.persistence.entity.BankAccountJpaEntity;
import bankingService.demo.adapter.out.persistence.entity.BeneficiaryJpaEntity;
import bankingService.demo.adapter.out.persistence.mapper.BeneficiaryPersistenceMapper;
import bankingService.demo.adapter.out.persistence.repository.BeneficiaryJpaRepository;
import bankingService.demo.domain.model.Beneficiary;
import bankingService.demo.domain.port.out.BeneficiaryRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BeneficiaryPersistenceAdapter implements BeneficiaryRepositoryPort {

    private final BeneficiaryJpaRepository repository;
    private final BeneficiaryPersistenceMapper mapper;
    private final EntityManager entityManager;

    public BeneficiaryPersistenceAdapter(BeneficiaryJpaRepository repository,
                                          BeneficiaryPersistenceMapper mapper,
                                          EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        BankAccountJpaEntity accountRef =
                entityManager.getReference(BankAccountJpaEntity.class, beneficiary.getOwnerAccountId());
        BeneficiaryJpaEntity entity = mapper.toEntity(beneficiary, accountRef);
        BeneficiaryJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Beneficiary> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByOwnerAccountIdAndBeneficiaryIban(UUID ownerAccountId, String beneficiaryIban) {
        return repository.existsByOwnerAccount_IdAndBeneficiaryIban(ownerAccountId, beneficiaryIban);
    }

    @Override
    public Page<Beneficiary> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Beneficiary> findByOwnerAccountId(UUID ownerAccountId) {
        return repository.findByOwnerAccount_Id(ownerAccountId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

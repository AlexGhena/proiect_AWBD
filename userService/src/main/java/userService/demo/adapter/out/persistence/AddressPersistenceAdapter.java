package userService.demo.adapter.out.persistence;

import userService.demo.adapter.out.persistence.entity.AddressJpaEntity;
import userService.demo.adapter.out.persistence.entity.UserProfileJpaEntity;
import userService.demo.adapter.out.persistence.mapper.AddressPersistenceMapper;
import userService.demo.adapter.out.persistence.repository.AddressJpaRepository;
import userService.demo.domain.model.Address;
import userService.demo.domain.port.out.AddressRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AddressPersistenceAdapter implements AddressRepositoryPort {

    private final AddressJpaRepository repository;
    private final AddressPersistenceMapper mapper;
    private final EntityManager entityManager;

    public AddressPersistenceAdapter(AddressJpaRepository repository,
                                      AddressPersistenceMapper mapper,
                                      EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Address save(Address address) {
        UserProfileJpaEntity profileRef =
                entityManager.getReference(UserProfileJpaEntity.class, address.getProfileId());
        AddressJpaEntity entity = mapper.toEntity(address, profileRef);
        AddressJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public Page<Address> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Address> findByProfileId(UUID profileId) {
        return repository.findByProfile_Id(profileId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

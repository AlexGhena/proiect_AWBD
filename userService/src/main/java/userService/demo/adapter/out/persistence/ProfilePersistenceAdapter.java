package userService.demo.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.adapter.out.persistence.entity.UserProfileJpaEntity;
import userService.demo.adapter.out.persistence.mapper.ProfilePersistenceMapper;
import userService.demo.adapter.out.persistence.repository.UserProfileJpaRepository;
import userService.demo.domain.model.UserProfile;
import userService.demo.domain.port.out.ProfileRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfilePersistenceAdapter implements ProfileRepositoryPort {

    private final UserProfileJpaRepository repository;
    private final ProfilePersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public UserProfile save(UserProfile profile) {
        AppUserJpaEntity userRef = entityManager.getReference(AppUserJpaEntity.class, profile.getUserId());
        UserProfileJpaEntity entity = mapper.toEntity(profile, userRef);
        UserProfileJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return repository.findByUser_Id(userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUser_Id(userId);
    }

    @Override
    public Page<UserProfile> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

package userService.demo.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.adapter.out.persistence.mapper.UserPersistenceMapper;
import userService.demo.adapter.out.persistence.repository.AppUserJpaRepository;
import userService.demo.domain.model.AppUser;
import userService.demo.domain.port.out.UserRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final AppUserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    @Override
    public AppUser save(AppUser user) {
        AppUserJpaEntity entity = mapper.toEntity(user);
        AppUserJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Page<AppUser> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

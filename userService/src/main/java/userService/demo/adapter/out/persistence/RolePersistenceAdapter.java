package userService.demo.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import userService.demo.adapter.out.persistence.entity.RoleJpaEntity;
import userService.demo.adapter.out.persistence.mapper.RolePersistenceMapper;
import userService.demo.adapter.out.persistence.repository.RoleJpaRepository;
import userService.demo.adapter.out.persistence.repository.UserRoleJpaRepository;
import userService.demo.domain.model.Role;
import userService.demo.domain.port.out.RoleRepositoryPort;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository repository;
    private final UserRoleJpaRepository userRoleJpaRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toEntity(role);
        RoleJpaEntity saved = repository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
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
    public boolean isReferenced(UUID roleId) {
        return userRoleJpaRepository.existsByRole_Id(roleId);
    }

    @Override
    public Page<Role> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}

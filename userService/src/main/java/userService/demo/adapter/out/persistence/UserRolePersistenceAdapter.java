package userService.demo.adapter.out.persistence;

import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.adapter.out.persistence.entity.RoleJpaEntity;
import userService.demo.adapter.out.persistence.entity.UserRoleId;
import userService.demo.adapter.out.persistence.entity.UserRoleJpaEntity;
import userService.demo.adapter.out.persistence.mapper.RolePersistenceMapper;
import userService.demo.adapter.out.persistence.repository.UserRoleJpaRepository;
import userService.demo.domain.model.Role;
import userService.demo.domain.port.out.UserRoleRepositoryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserRolePersistenceAdapter implements UserRoleRepositoryPort {

    private final UserRoleJpaRepository repository;
    private final RolePersistenceMapper roleMapper;
    private final EntityManager entityManager;

    public UserRolePersistenceAdapter(UserRoleJpaRepository repository,
                                       RolePersistenceMapper roleMapper,
                                       EntityManager entityManager) {
        this.repository = repository;
        this.roleMapper = roleMapper;
        this.entityManager = entityManager;
    }

    @Override
    public void assign(UUID userId, UUID roleId) {
        AppUserJpaEntity userRef = entityManager.getReference(AppUserJpaEntity.class, userId);
        RoleJpaEntity roleRef = entityManager.getReference(RoleJpaEntity.class, roleId);
        UserRoleJpaEntity link = UserRoleJpaEntity.builder()
                .id(new UserRoleId(userId, roleId))
                .user(userRef)
                .role(roleRef)
                .build();
        repository.saveAndFlush(link);
    }

    @Override
    public void unassign(UUID userId, UUID roleId) {
        repository.deleteById(new UserRoleId(userId, roleId));
    }

    @Override
    public boolean isAssigned(UUID userId, UUID roleId) {
        return repository.existsById(new UserRoleId(userId, roleId));
    }

    @Override
    public List<Role> findRolesForUser(UUID userId) {
        return repository.findById_UserId(userId).stream()
                .map(link -> roleMapper.toDomain(link.getRole()))
                .collect(Collectors.toList());
    }
}

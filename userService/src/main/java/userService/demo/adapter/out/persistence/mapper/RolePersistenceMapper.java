package userService.demo.adapter.out.persistence.mapper;

import userService.demo.adapter.out.persistence.entity.RoleJpaEntity;
import userService.demo.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceMapper {

    public RoleJpaEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }
        return RoleJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .build();
    }

    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}

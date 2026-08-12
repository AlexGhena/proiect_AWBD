package userService.demo.adapter.out.persistence.mapper;

import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.domain.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public AppUserJpaEntity toEntity(AppUser domain) {
        if (domain == null) {
            return null;
        }
        return AppUserJpaEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .enabled(domain.getEnabled())
                .approvalStatus(domain.getApprovalStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    public AppUser toDomain(AppUserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AppUser.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .enabled(entity.getEnabled())
                .approvalStatus(entity.getApprovalStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}

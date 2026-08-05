package userService.demo.adapter.out.persistence.mapper;

import userService.demo.adapter.out.persistence.entity.AppUserJpaEntity;
import userService.demo.adapter.out.persistence.entity.UserProfileJpaEntity;
import userService.demo.domain.model.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class ProfilePersistenceMapper {

    public UserProfileJpaEntity toEntity(UserProfile domain, AppUserJpaEntity userRef) {
        if (domain == null) {
            return null;
        }
        return UserProfileJpaEntity.builder()
                .id(domain.getId())
                .user(userRef)
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .phone(domain.getPhone())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public UserProfile toDomain(UserProfileJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserProfile.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

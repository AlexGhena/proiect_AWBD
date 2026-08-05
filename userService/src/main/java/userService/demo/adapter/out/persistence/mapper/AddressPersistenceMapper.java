package userService.demo.adapter.out.persistence.mapper;

import userService.demo.adapter.out.persistence.entity.AddressJpaEntity;
import userService.demo.adapter.out.persistence.entity.UserProfileJpaEntity;
import userService.demo.domain.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressPersistenceMapper {

    public AddressJpaEntity toEntity(Address domain, UserProfileJpaEntity profileRef) {
        if (domain == null) {
            return null;
        }
        return AddressJpaEntity.builder()
                .id(domain.getId())
                .profile(profileRef)
                .label(domain.getLabel())
                .street(domain.getStreet())
                .city(domain.getCity())
                .postalCode(domain.getPostalCode())
                .country(domain.getCountry())
                .isDefault(domain.getIsDefault())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Address toDomain(AddressJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Address.builder()
                .id(entity.getId())
                .profileId(entity.getProfile().getId())
                .label(entity.getLabel())
                .street(entity.getStreet())
                .city(entity.getCity())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

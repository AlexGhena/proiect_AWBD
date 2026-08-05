package userService.demo.adapter.in.web.mapper;

import userService.demo.adapter.in.web.dto.address.AddressResponse;
import userService.demo.adapter.in.web.dto.address.CreateAddressRequest;
import userService.demo.adapter.in.web.dto.address.UpdateAddressRequest;
import userService.demo.domain.model.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressWebMapper {

    public Address toDomain(CreateAddressRequest request) {
        return Address.builder()
                .profileId(request.profileId())
                .label(request.label())
                .street(request.street())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(request.isDefault())
                .build();
    }

    public Address toDomain(UpdateAddressRequest request) {
        return Address.builder()
                .label(request.label())
                .street(request.street())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(request.isDefault())
                .build();
    }

    public AddressResponse toResponse(Address domain) {
        return new AddressResponse(
                domain.getId(),
                domain.getProfileId(),
                domain.getLabel(),
                domain.getStreet(),
                domain.getCity(),
                domain.getPostalCode(),
                domain.getCountry(),
                domain.getIsDefault(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

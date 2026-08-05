package userService.demo.adapter.in.web.dto.address;

import userService.demo.domain.model.AddressLabel;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID profileId,
        AddressLabel label,
        String street,
        String city,
        String postalCode,
        String country,
        Boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
}

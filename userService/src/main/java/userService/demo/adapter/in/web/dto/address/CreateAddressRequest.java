package userService.demo.adapter.in.web.dto.address;

import userService.demo.domain.model.AddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAddressRequest(
        @NotNull UUID profileId,
        AddressLabel label,
        @NotBlank @Size(max = 150) String street,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "country must be a 2-letter ISO code") String country,
        Boolean isDefault
) {
}

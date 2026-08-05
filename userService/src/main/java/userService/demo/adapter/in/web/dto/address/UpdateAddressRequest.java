package userService.demo.adapter.in.web.dto.address;

import userService.demo.domain.model.AddressLabel;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        AddressLabel label,
        @Size(max = 150) String street,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Pattern(regexp = "^[A-Z]{2}$", message = "country must be a 2-letter ISO code") String country,
        Boolean isDefault
) {
}

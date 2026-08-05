package userService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private UUID id;
    private UUID profileId;
    private AddressLabel label;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}

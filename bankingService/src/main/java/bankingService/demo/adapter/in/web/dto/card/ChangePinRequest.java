package bankingService.demo.adapter.in.web.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank String currentPassword,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "newPin must be exactly 4 digits") String newPin
) {
}

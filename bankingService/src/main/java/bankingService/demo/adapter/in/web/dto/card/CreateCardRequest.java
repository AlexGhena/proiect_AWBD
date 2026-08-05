package bankingService.demo.adapter.in.web.dto.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull UUID accountId,
        @NotBlank String cardReference,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "lastFour must be exactly 4 digits") String lastFour,
        @NotBlank String cardholderName,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull Integer expiryYear
) {
}

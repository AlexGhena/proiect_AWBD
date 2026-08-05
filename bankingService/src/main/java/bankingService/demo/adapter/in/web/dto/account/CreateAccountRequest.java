package bankingService.demo.adapter.in.web.dto.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID userId,
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{15,34}$", message = "iban must be 15-34 uppercase alphanumeric characters")
        String iban,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @DecimalMin(value = "0.0", message = "balance must not be negative") BigDecimal balance
) {
}

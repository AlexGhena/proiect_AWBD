package bankingService.demo.adapter.in.web.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ProvisionAccountRequest(
        @NotNull UUID userId,
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency
) {
    private static final String DEFAULT_CURRENCY = "RON";

    public ProvisionAccountRequest {
        if (currency == null || currency.isBlank()) {
            currency = DEFAULT_CURRENCY;
        }
    }
}

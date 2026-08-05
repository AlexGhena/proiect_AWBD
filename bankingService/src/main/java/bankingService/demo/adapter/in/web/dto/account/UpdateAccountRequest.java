package bankingService.demo.adapter.in.web.dto.account;

import bankingService.demo.domain.model.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateAccountRequest(
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @DecimalMin(value = "0.0", message = "balance must not be negative") BigDecimal balance,
        AccountStatus status
) {
}

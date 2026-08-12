package transactionService.demo.adapter.in.web.dto.transaction;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID categoryId,
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        String description
) {
    @AssertTrue(message = "source and destination accounts must be different")
    public boolean isAccountPairValid() {
        return sourceAccountId == null || destinationAccountId == null || !sourceAccountId.equals(destinationAccountId);
    }
}

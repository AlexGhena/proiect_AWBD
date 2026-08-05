package transactionService.demo.adapter.in.web.dto.transaction;

import transactionService.demo.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        UUID categoryId,
        UUID scheduledTransactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull TransactionType type,
        String description
) {
}

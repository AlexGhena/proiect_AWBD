package transactionService.demo.adapter.in.web.dto.scheduled;

import transactionService.demo.domain.model.ScheduleFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateScheduledTransactionRequest(
        UUID categoryId,
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull ScheduleFrequency frequency,
        @NotNull @FutureOrPresent LocalDate nextExecutionDate,
        String description
) {
}

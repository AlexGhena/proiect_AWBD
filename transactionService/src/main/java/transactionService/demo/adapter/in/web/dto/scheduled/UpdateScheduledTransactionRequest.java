package transactionService.demo.adapter.in.web.dto.scheduled;

import transactionService.demo.domain.model.ScheduleFrequency;
import transactionService.demo.domain.model.ScheduleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateScheduledTransactionRequest(
        UUID categoryId,
        @DecimalMin(value = "0.01") BigDecimal amount,
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        ScheduleFrequency frequency,
        LocalDate nextExecutionDate,
        ScheduleStatus status,
        String description
) {
}

package transactionService.demo.adapter.in.web.dto.scheduled;

import transactionService.demo.domain.model.ScheduleFrequency;
import transactionService.demo.domain.model.ScheduleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledTransactionResponse(
        UUID id,
        UUID categoryId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        ScheduleFrequency frequency,
        LocalDate nextExecutionDate,
        ScheduleStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}

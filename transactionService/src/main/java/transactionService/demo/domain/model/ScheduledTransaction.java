package transactionService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransaction {

    private UUID id;
    private UUID categoryId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private ScheduleFrequency frequency;
    private LocalDate nextExecutionDate;
    private ScheduleStatus status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}

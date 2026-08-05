package transactionService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {

    private UUID id;
    private UUID categoryId;
    private UUID scheduledTransactionId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private UUID sagaId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}

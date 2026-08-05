package transactionService.demo.adapter.in.web.dto.transaction;

import transactionService.demo.domain.model.TransactionStatus;
import transactionService.demo.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID categoryId,
        UUID scheduledTransactionId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        UUID sagaId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        TransactionStatus status,
        String description,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}

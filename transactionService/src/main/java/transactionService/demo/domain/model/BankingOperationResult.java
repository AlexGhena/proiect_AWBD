package transactionService.demo.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BankingOperationResult(UUID accountId, BigDecimal balance, boolean replayed) {
}

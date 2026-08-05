package transactionService.demo.adapter.in.web.dto.transaction;

import transactionService.demo.domain.model.TransactionStatus;

public record UpdateTransactionRequest(
        TransactionStatus status,
        String description,
        String failureReason
) {
}

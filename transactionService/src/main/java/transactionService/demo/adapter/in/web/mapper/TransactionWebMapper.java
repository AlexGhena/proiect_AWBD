package transactionService.demo.adapter.in.web.mapper;

import transactionService.demo.adapter.in.web.dto.transaction.CreateTransactionRequest;
import transactionService.demo.adapter.in.web.dto.transaction.TransactionResponse;
import transactionService.demo.adapter.in.web.dto.transaction.TransferRequest;
import transactionService.demo.adapter.in.web.dto.transaction.UpdateTransactionRequest;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.model.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    public BankTransaction toDomain(CreateTransactionRequest request) {
        return BankTransaction.builder()
                .categoryId(request.categoryId())
                .scheduledTransactionId(request.scheduledTransactionId())
                .sourceAccountId(request.sourceAccountId())
                .destinationAccountId(request.destinationAccountId())
                .amount(request.amount())
                .currency(request.currency())
                .type(request.type())
                .description(request.description())
                .build();
    }

    public BankTransaction toDomain(TransferRequest request) {
        return BankTransaction.builder()
                .categoryId(request.categoryId())
                .sourceAccountId(request.sourceAccountId())
                .destinationAccountId(request.destinationAccountId())
                .amount(request.amount())
                .currency(request.currency())
                .type(TransactionType.TRANSFER)
                .description(request.description())
                .build();
    }

    public BankTransaction toDomain(UpdateTransactionRequest request) {
        return BankTransaction.builder()
                .status(request.status())
                .description(request.description())
                .failureReason(request.failureReason())
                .build();
    }

    public TransactionResponse toResponse(BankTransaction domain) {
        return new TransactionResponse(
                domain.getId(),
                domain.getCategoryId(),
                domain.getScheduledTransactionId(),
                domain.getSourceAccountId(),
                domain.getDestinationAccountId(),
                domain.getSagaId(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getType(),
                domain.getStatus(),
                domain.getDescription(),
                domain.getFailureReason(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

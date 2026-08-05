package transactionService.demo.adapter.in.web.mapper;

import transactionService.demo.adapter.in.web.dto.scheduled.CreateScheduledTransactionRequest;
import transactionService.demo.adapter.in.web.dto.scheduled.ScheduledTransactionResponse;
import transactionService.demo.adapter.in.web.dto.scheduled.UpdateScheduledTransactionRequest;
import transactionService.demo.domain.model.ScheduledTransaction;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTransactionWebMapper {

    public ScheduledTransaction toDomain(CreateScheduledTransactionRequest request) {
        return ScheduledTransaction.builder()
                .categoryId(request.categoryId())
                .sourceAccountId(request.sourceAccountId())
                .destinationAccountId(request.destinationAccountId())
                .amount(request.amount())
                .currency(request.currency())
                .frequency(request.frequency())
                .nextExecutionDate(request.nextExecutionDate())
                .description(request.description())
                .build();
    }

    public ScheduledTransaction toDomain(UpdateScheduledTransactionRequest request) {
        return ScheduledTransaction.builder()
                .categoryId(request.categoryId())
                .amount(request.amount())
                .currency(request.currency())
                .frequency(request.frequency())
                .nextExecutionDate(request.nextExecutionDate())
                .status(request.status())
                .description(request.description())
                .build();
    }

    public ScheduledTransactionResponse toResponse(ScheduledTransaction domain) {
        return new ScheduledTransactionResponse(
                domain.getId(),
                domain.getCategoryId(),
                domain.getSourceAccountId(),
                domain.getDestinationAccountId(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getFrequency(),
                domain.getNextExecutionDate(),
                domain.getStatus(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

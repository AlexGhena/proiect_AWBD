package transactionService.demo.adapter.in.web;

import transactionService.demo.adapter.in.web.dto.transaction.CreateTransactionRequest;
import transactionService.demo.adapter.in.web.dto.transaction.TransactionResponse;
import transactionService.demo.adapter.in.web.dto.transaction.UpdateTransactionRequest;
import transactionService.demo.adapter.in.web.mapper.TransactionWebMapper;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.port.in.BankTransactionUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class TransactionController {

    private final BankTransactionUseCase bankTransactionUseCase;
    private final TransactionWebMapper mapper;

    public TransactionController(BankTransactionUseCase bankTransactionUseCase, TransactionWebMapper mapper) {
        this.bankTransactionUseCase = bankTransactionUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/api/transactions")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        BankTransaction created = bankTransactionUseCase.createTransaction(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(bankTransactionUseCase.getTransaction(id)));
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<PagedModel<TransactionResponse>> list(Pageable pageable) {
        Page<TransactionResponse> page = bankTransactionUseCase.listTransactions(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/api/scheduled-transactions/{scheduledTransactionId}/transactions")
    public ResponseEntity<List<TransactionResponse>> listBySchedule(@PathVariable UUID scheduledTransactionId) {
        List<TransactionResponse> transactions =
                bankTransactionUseCase.listTransactionsBySchedule(scheduledTransactionId).stream()
                        .map(mapper::toResponse)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/api/transactions/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateTransactionRequest request) {
        BankTransaction updated = bankTransactionUseCase.updateTransaction(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bankTransactionUseCase.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}

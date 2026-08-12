package transactionService.demo.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import transactionService.demo.adapter.in.web.dto.common.PageResponse;
import transactionService.demo.adapter.in.web.dto.transaction.CreateTransactionRequest;
import transactionService.demo.adapter.in.web.dto.transaction.TransactionResponse;
import transactionService.demo.adapter.in.web.dto.transaction.UpdateTransactionRequest;
import transactionService.demo.adapter.in.web.mapper.TransactionWebMapper;
import transactionService.demo.adapter.in.web.support.PaginationParamsResolver;
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.port.in.BankTransactionUseCase;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private static final Set<String> SORT_FIELDS = Set.of("amount", "status", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final BankTransactionUseCase bankTransactionUseCase;
    private final TransactionWebMapper mapper;
    private final PaginationParamsResolver paginationParamsResolver;

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
    public ResponseEntity<PageResponse<TransactionResponse>> list(@RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) String sortBy,
                                                                     @RequestParam(required = false) String sortDirection) {
        Pageable pageable = paginationParamsResolver.resolve(page, size, sortBy, sortDirection, SORT_FIELDS, DEFAULT_SORT_FIELD);
        Page<TransactionResponse> result = bankTransactionUseCase.listTransactions(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
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

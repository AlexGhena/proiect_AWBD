package transactionService.demo.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import transactionService.demo.adapter.in.web.dto.scheduled.CreateScheduledTransactionRequest;
import transactionService.demo.adapter.in.web.dto.scheduled.ScheduledTransactionResponse;
import transactionService.demo.adapter.in.web.dto.scheduled.UpdateScheduledTransactionRequest;
import transactionService.demo.adapter.in.web.mapper.ScheduledTransactionWebMapper;
import transactionService.demo.domain.model.ScheduledTransaction;
import transactionService.demo.domain.port.in.ScheduledTransactionUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/scheduled-transactions")
@RequiredArgsConstructor
public class ScheduledTransactionController {

    private final ScheduledTransactionUseCase scheduledTransactionUseCase;
    private final ScheduledTransactionWebMapper mapper;

    @PostMapping
    public ResponseEntity<ScheduledTransactionResponse> create(
            @Valid @RequestBody CreateScheduledTransactionRequest request) {
        ScheduledTransaction created =
                scheduledTransactionUseCase.createScheduledTransaction(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTransactionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(scheduledTransactionUseCase.getScheduledTransaction(id)));
    }

    @GetMapping
    public ResponseEntity<PagedModel<ScheduledTransactionResponse>> list(Pageable pageable) {
        Page<ScheduledTransactionResponse> page =
                scheduledTransactionUseCase.listScheduledTransactions(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledTransactionResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateScheduledTransactionRequest request) {
        ScheduledTransaction updated =
                scheduledTransactionUseCase.updateScheduledTransaction(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduledTransactionUseCase.deleteScheduledTransaction(id);
        return ResponseEntity.noContent().build();
    }
}

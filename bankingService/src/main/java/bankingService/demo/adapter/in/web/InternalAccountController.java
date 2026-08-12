package bankingService.demo.adapter.in.web;

import bankingService.demo.adapter.in.web.dto.internal.AccountOperationResponse;
import bankingService.demo.adapter.in.web.dto.internal.AccountSnapshotResponse;
import bankingService.demo.adapter.in.web.dto.internal.CompensateAccountRequest;
import bankingService.demo.adapter.in.web.dto.internal.CreditAccountRequest;
import bankingService.demo.adapter.in.web.dto.internal.DebitAccountRequest;
import bankingService.demo.domain.model.AccountOperationResult;
import bankingService.demo.domain.model.AccountSnapshot;
import bankingService.demo.domain.port.in.AccountTransferUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Balance operations for the transfer Saga. Reached only by other services over the internal
 * network - never routed publicly by Ingress - so authorization here is "any authenticated
 * caller", not resource ownership. See {@link AccountTransferUseCase}.
 */
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountTransferUseCase accountTransferUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<AccountSnapshotResponse> getSnapshot(@PathVariable UUID id) {
        AccountSnapshot snapshot = accountTransferUseCase.getSnapshot(id);
        return ResponseEntity.ok(new AccountSnapshotResponse(snapshot.getId(), snapshot.getCurrency(), snapshot.getStatus()));
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<AccountOperationResponse> debit(@PathVariable UUID id,
                                                            @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                            @Valid @RequestBody DebitAccountRequest request) {
        AccountOperationResult result = accountTransferUseCase.debit(
                id, request.amount(), request.currency(), request.sagaId(), idempotencyKey);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<AccountOperationResponse> credit(@PathVariable UUID id,
                                                             @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                             @Valid @RequestBody CreditAccountRequest request) {
        AccountOperationResult result = accountTransferUseCase.credit(
                id, request.amount(), request.currency(), request.sagaId(), idempotencyKey);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/compensate")
    public ResponseEntity<AccountOperationResponse> compensate(@PathVariable UUID id,
                                                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                 @Valid @RequestBody CompensateAccountRequest request) {
        AccountOperationResult result = accountTransferUseCase.compensate(
                id, request.originalIdempotencyKey(), request.sagaId(), idempotencyKey);
        return ResponseEntity.ok(toResponse(result));
    }

    private AccountOperationResponse toResponse(AccountOperationResult result) {
        return new AccountOperationResponse(result.getAccountId(), result.getBalance(), result.isReplayed());
    }
}

package bankingService.demo.adapter.in.web;

import bankingService.demo.adapter.in.web.dto.account.AccountResponse;
import bankingService.demo.adapter.in.web.dto.account.CreateAccountRequest;
import bankingService.demo.adapter.in.web.dto.account.UpdateAccountRequest;
import bankingService.demo.adapter.in.web.mapper.AccountWebMapper;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.port.in.AccountUseCase;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountUseCase accountUseCase;
    private final AccountWebMapper mapper;

    public AccountController(AccountUseCase accountUseCase, AccountWebMapper mapper) {
        this.accountUseCase = accountUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        BankAccount created = accountUseCase.createAccount(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(accountUseCase.getAccount(id)));
    }

    @GetMapping
    public ResponseEntity<PagedModel<AccountResponse>> list(Pageable pageable) {
        Page<AccountResponse> page = accountUseCase.listAccounts(pageable).map(mapper::toResponse);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateAccountRequest request) {
        BankAccount updated = accountUseCase.updateAccount(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountUseCase.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}

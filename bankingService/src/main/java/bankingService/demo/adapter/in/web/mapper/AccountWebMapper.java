package bankingService.demo.adapter.in.web.mapper;

import bankingService.demo.adapter.in.web.dto.account.AccountResponse;
import bankingService.demo.adapter.in.web.dto.account.CreateAccountRequest;
import bankingService.demo.adapter.in.web.dto.account.UpdateAccountRequest;
import bankingService.demo.domain.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class AccountWebMapper {

    public BankAccount toDomain(CreateAccountRequest request) {
        return BankAccount.builder()
                .userId(request.userId())
                .iban(request.iban())
                .currency(request.currency())
                .balance(request.balance())
                .build();
    }

    public BankAccount toDomain(UpdateAccountRequest request) {
        return BankAccount.builder()
                .currency(request.currency())
                .balance(request.balance())
                .status(request.status())
                .build();
    }

    public AccountResponse toResponse(BankAccount domain) {
        return new AccountResponse(
                domain.getId(),
                domain.getUserId(),
                domain.getIban(),
                domain.getCurrency(),
                domain.getBalance(),
                domain.getStatus(),
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

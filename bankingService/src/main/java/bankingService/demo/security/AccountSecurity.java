package bankingService.demo.security;

import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.domain.port.out.BeneficiaryRepositoryPort;
import bankingService.demo.domain.port.out.CardRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resource-ownership checks referenced from {@code @PreAuthorize} as {@code @accountSecurity}.
 *
 * <p>Accounts carry the owning user's UUID directly; cards and beneficiaries inherit ownership from
 * the account they hang off. Every comparison is against the {@code sub} claim of the caller's token,
 * never against anything the request supplied.
 */
@Component("accountSecurity")
@RequiredArgsConstructor
public class AccountSecurity {

    private final AuthenticatedUserResolver resolver;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final BeneficiaryRepositoryPort beneficiaryRepositoryPort;

    @Transactional(readOnly = true)
    public boolean ownsAccount(UUID accountId) {
        return resolver.current()
                .flatMap(user -> accountRepositoryPort.findById(accountId)
                        .map(account -> account.getUserId().equals(user.id())))
                .orElse(false);
    }

    /** A caller may open an account only in their own name. */
    public boolean isSelf(UUID userId) {
        return resolver.current()
                .map(user -> user.id().equals(userId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsCard(UUID cardId) {
        return cardRepositoryPort.findById(cardId)
                .map(card -> ownsAccount(card.getAccountId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsBeneficiary(UUID beneficiaryId) {
        return beneficiaryRepositoryPort.findById(beneficiaryId)
                .map(beneficiary -> ownsAccount(beneficiary.getOwnerAccountId()))
                .orElse(false);
    }
}

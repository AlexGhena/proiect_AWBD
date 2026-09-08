package bankingService.demo.adapter.in.web.dto.account;

import bankingService.demo.domain.model.AccountStatus;

import java.util.UUID;

/**
 * Minimal, non-sensitive view of an account returned by the IBAN lookup used to pick a transfer
 * destination. Intentionally omits balance, owner and version so resolving someone else's IBAN
 * never discloses their account details.
 */
public record AccountLookupResponse(
        UUID id,
        String iban,
        String currency,
        AccountStatus status
) {
}

package bankingService.demo.adapter.in.web.dto.account;

import bankingService.demo.domain.model.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID userId,
        String iban,
        String currency,
        BigDecimal balance,
        AccountStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}

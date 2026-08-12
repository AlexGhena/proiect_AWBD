package bankingService.demo.adapter.in.web.dto.internal;

import bankingService.demo.domain.model.AccountStatus;

import java.util.UUID;

public record AccountSnapshotResponse(
        UUID id,
        String currency,
        AccountStatus status
) {
}

package bankingService.demo.adapter.in.web.dto.beneficiary;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
        UUID id,
        UUID ownerAccountId,
        String beneficiaryName,
        String beneficiaryIban,
        String nickname,
        Instant createdAt,
        Instant updatedAt
) {
}

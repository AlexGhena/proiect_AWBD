package bankingService.demo.adapter.in.web.dto.internal;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountOperationResponse(
        UUID accountId,
        BigDecimal balance,
        boolean replayed
) {
}

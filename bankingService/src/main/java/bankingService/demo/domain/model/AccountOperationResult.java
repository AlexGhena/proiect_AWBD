package bankingService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOperationResult {

    private UUID accountId;
    private BigDecimal balance;
    /** True when this call returned a previously recorded result instead of applying a new mutation. */
    private boolean replayed;
}

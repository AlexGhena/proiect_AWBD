package bankingService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

    private UUID id;
    private UUID userId;
    private String iban;
    private String currency;
    private BigDecimal balance;
    private AccountStatus status;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}

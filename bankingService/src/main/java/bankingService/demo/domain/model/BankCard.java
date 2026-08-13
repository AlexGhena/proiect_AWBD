package bankingService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCard {

    private UUID id;
    private UUID accountId;
    private String cardReference;
    private String lastFour;
    private String cardholderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private CardStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /** Full PAN, CVV and PIN - decrypted in memory by the persistence mapper, never logged. */
    @ToString.Exclude
    private String cardNumber;
    @ToString.Exclude
    private String cvv;
    @ToString.Exclude
    private String pin;
}

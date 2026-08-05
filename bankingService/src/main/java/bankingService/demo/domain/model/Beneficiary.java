package bankingService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {

    private UUID id;
    private UUID ownerAccountId;
    private String beneficiaryName;
    private String beneficiaryIban;
    private String nickname;
    private Instant createdAt;
    private Instant updatedAt;
}

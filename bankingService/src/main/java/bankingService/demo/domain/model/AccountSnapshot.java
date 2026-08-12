package bankingService.demo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Minimal account view exposed to the transfer Saga for pre-flight validation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSnapshot {

    private UUID id;
    private String currency;
    private AccountStatus status;
}

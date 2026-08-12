package transactionService.demo.domain.model;

import java.util.UUID;

/** What the transfer Saga needs to know about an account before touching it. */
public record BankingAccountSnapshot(UUID id, String currency, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}

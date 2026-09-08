package bankingService.demo;

import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.support.BankingIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract of {@code GET /api/accounts/resolve} - IBAN lookup used to pick a transfer destination. */
class AccountIbanLookupTests extends BankingIntegrationTest {

    @Test
    @DisplayName("A user can resolve another user's IBAN, seeing only id/currency/status - never balance or owner")
    void resolveExposesOnlyNonSensitiveFields() throws Exception {
        BankAccount other = saveAccount(UUID.randomUUID(), BigDecimal.valueOf(1234.56));

        mockMvc.perform(get("/api/accounts/resolve")
                        .param("iban", other.getIban())
                        .header("Authorization", userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(other.getId().toString()))
                .andExpect(jsonPath("$.iban").value(other.getIban()))
                .andExpect(jsonPath("$.currency").value("RON"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    @DisplayName("Lookup normalizes the IBAN, so a lowercase query still resolves")
    void resolveNormalizesCasing() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.ZERO);

        mockMvc.perform(get("/api/accounts/resolve")
                        .param("iban", account.getIban().toLowerCase())
                        .header("Authorization", userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()));
    }

    @Test
    @DisplayName("An unknown IBAN returns 404")
    void unknownIbanIsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/resolve")
                        .param("iban", "RO00UNKNOWN0000000000000")
                        .header("Authorization", userBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated lookup is rejected with 401")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/resolve").param("iban", "RO00UNKNOWN0000000000000"))
                .andExpect(status().isUnauthorized());
    }
}

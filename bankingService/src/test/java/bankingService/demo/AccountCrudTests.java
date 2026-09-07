package bankingService.demo;

import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.support.BankingIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full CRUD + authorization contract of {@code /api/accounts}. */
class AccountCrudTests extends BankingIntegrationTest {

    private String uniqueIban() {
        String iban = ("ACC" + UUID.randomUUID()).toUpperCase().replaceAll("[^A-Z0-9]", "");
        return iban.substring(0, 24);
    }

    private JsonNode createAccount(String bearer, UUID ownerId) throws Exception {
        return json(mockMvc.perform(post("/api/accounts")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"%s","currency":"RON","balance":100.00}
                                """.formatted(ownerId, uniqueIban())))
                .andExpect(status().isCreated())
                .andReturn());
    }

    @Test
    @DisplayName("Admin can open an account, which is fetchable and auto-issues a debit card")
    void createAndFetch() throws Exception {
        String bearer = adminBearer();

        JsonNode created = createAccount(bearer, USER_ID);
        String id = created.get("id").asString();

        mockMvc.perform(get("/api/accounts/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("RON"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/accounts/{accountId}/cards", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("A user may open an account only in their own name")
    void userOpensOwnAccount() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", userBearer())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"%s","currency":"RON","balance":0.00}
                                """.formatted(USER_ID, uniqueIban())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A user cannot open an account in another user's name")
    void userCannotOpenForOthers() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", userBearer())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"%s","currency":"RON","balance":0.00}
                                """.formatted(UUID.randomUUID(), uniqueIban())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A duplicate IBAN is rejected with 409")
    void duplicateIbanIsConflict() throws Exception {
        String bearer = adminBearer();
        String iban = uniqueIban();

        mockMvc.perform(post("/api/accounts").header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"%s","currency":"RON","balance":0.00}
                                """.formatted(USER_ID, iban)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/accounts").header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"%s","currency":"RON","balance":0.00}
                                """.formatted(USER_ID, iban)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A malformed IBAN is rejected with 400")
    void malformedIbanIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts").header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","iban":"short","currency":"RON","balance":0.00}
                                """.formatted(USER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/accounts/me returns only the caller's own accounts")
    void listMineIsScopedToCaller() throws Exception {
        saveAccount(USER_ID, BigDecimal.valueOf(50));
        saveAccount(UUID.randomUUID(), BigDecimal.valueOf(50));

        mockMvc.perform(get("/api/accounts/me").header("Authorization", userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.userId != '" + USER_ID + "')]").doesNotExist());
    }

    @Test
    @DisplayName("An owner can update their account currency but not its balance")
    void ownerUpdateCurrencyNotBalance() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));

        mockMvc.perform(put("/api/accounts/{id}", account.getId())
                        .header("Authorization", userBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"EUR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"));

        mockMvc.perform(put("/api/accounts/{id}", account.getId())
                        .header("Authorization", userBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"balance":999999.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An admin can set the balance directly")
    void adminUpdatesBalance() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));

        mockMvc.perform(put("/api/accounts/{id}", account.getId())
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"balance":500.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    @DisplayName("A user cannot read an account they do not own")
    void userCannotReadOthersAccount() throws Exception {
        BankAccount account = saveAccount(UUID.randomUUID(), BigDecimal.valueOf(100));

        mockMvc.perform(get("/api/accounts/{id}", account.getId()).header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An owner can delete their account")
    void ownerDeletesAccount() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.ZERO);

        mockMvc.perform(delete("/api/accounts/{id}", account.getId())
                        .header("Authorization", userBearer()).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/{id}", account.getId()).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Fetching an unknown account returns 404")
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", UUID.randomUUID()).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated access is rejected with 401")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }
}

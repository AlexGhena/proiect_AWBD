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

/** Full CRUD + authorization contract of the beneficiary endpoints. */
class BeneficiaryCrudTests extends BankingIntegrationTest {

    private String uniqueIban() {
        String iban = ("BEN" + UUID.randomUUID()).toUpperCase().replaceAll("[^A-Z0-9]", "");
        return iban.substring(0, 24);
    }

    private String createBeneficiary(String bearer, UUID accountId, String iban) throws Exception {
        JsonNode created = json(mockMvc.perform(post("/api/beneficiaries")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerAccountId":"%s","beneficiaryName":"Mihai Radulescu",
                                 "beneficiaryIban":"%s","nickname":"Rent"}
                                """.formatted(accountId, iban)))
                .andExpect(status().isCreated())
                .andReturn());
        return created.get("id").asString();
    }

    @Test
    @DisplayName("Owner can create, fetch, update and delete a beneficiary")
    void fullLifecycle() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();

        String id = createBeneficiary(bearer, account.getId(), uniqueIban());

        mockMvc.perform(get("/api/beneficiaries/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiaryName").value("Mihai Radulescu"));

        mockMvc.perform(put("/api/beneficiaries/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Landlord"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Landlord"));

        mockMvc.perform(get("/api/accounts/{accountId}/beneficiaries", account.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(delete("/api/beneficiaries/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/beneficiaries/{id}", id).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The same beneficiary IBAN cannot be saved twice on one account")
    void duplicateBeneficiaryIsConflict() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();
        String iban = uniqueIban();
        createBeneficiary(bearer, account.getId(), iban);

        mockMvc.perform(post("/api/beneficiaries")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerAccountId":"%s","beneficiaryName":"Dup","beneficiaryIban":"%s"}
                                """.formatted(account.getId(), iban)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A beneficiary on an unknown account returns 404")
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(post("/api/beneficiaries")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerAccountId":"%s","beneficiaryName":"Nobody","beneficiaryIban":"%s"}
                                """.formatted(UUID.randomUUID(), uniqueIban())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A malformed beneficiary IBAN is rejected with 400")
    void malformedIbanIsBadRequest() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/beneficiaries")
                        .header("Authorization", userBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerAccountId":"%s","beneficiaryName":"Bad","beneficiaryIban":"lowercase"}
                                """.formatted(account.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A user cannot add a beneficiary to another user's account")
    void userCannotUseOthersAccount() throws Exception {
        BankAccount account = saveAccount(UUID.randomUUID(), BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/beneficiaries")
                        .header("Authorization", userBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerAccountId":"%s","beneficiaryName":"Sneaky","beneficiaryIban":"%s"}
                                """.formatted(account.getId(), uniqueIban())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Only an admin may list all beneficiaries")
    void listAllIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/beneficiaries").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/beneficiaries").header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }
}

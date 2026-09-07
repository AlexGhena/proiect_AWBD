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

/** Full CRUD, secret-reveal and lifecycle contract of the card endpoints. */
class CardCrudTests extends BankingIntegrationTest {

    private String createCard(String bearer, UUID accountId) throws Exception {
        JsonNode created = json(mockMvc.perform(post("/api/cards")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","cardReference":"REF-%s","lastFour":"4321",
                                 "cardholderName":"Elena Dumitrescu","expiryMonth":5,"expiryYear":2030}
                                """.formatted(accountId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastFour").value("4321"))
                .andReturn());
        return created.get("id").asString();
    }

    @Test
    @DisplayName("Owner can create, fetch, update and delete a card")
    void fullLifecycle() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();

        String id = createCard(bearer, account.getId());

        mockMvc.perform(get("/api/cards/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("Elena Dumitrescu"));

        mockMvc.perform(put("/api/cards/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardholderName":"Elena D.","expiryYear":2031}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("Elena D."))
                .andExpect(jsonPath("$.expiryYear").value(2031));

        mockMvc.perform(delete("/api/cards/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", id).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A card is never issued against a non-existent account")
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","cardReference":"REF-X","lastFour":"0000",
                                 "cardholderName":"Ghost","expiryMonth":1,"expiryYear":2030}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A non-4-digit lastFour is rejected with 400")
    void invalidLastFourIsBadRequest() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","cardReference":"REF-Y","lastFour":"12",
                                 "cardholderName":"Elena","expiryMonth":1,"expiryYear":2030}
                                """.formatted(account.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Owner can reveal card details and PIN with their password")
    void revealDetailsAndPin() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();
        String id = createCard(bearer, account.getId());

        mockMvc.perform(post("/api/cards/{id}/reveal", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Passw0rd!23"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").exists());

        mockMvc.perform(post("/api/cards/{id}/pin/reveal", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Passw0rd!23"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pin").exists());
    }

    @Test
    @DisplayName("Revealing with the wrong password is rejected")
    void revealWithWrongPasswordIsDenied() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();
        String id = createCard(bearer, account.getId());

        org.mockito.Mockito.when(userServiceClient.verifyPassword(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                .thenReturn(false);

        mockMvc.perform(post("/api/cards/{id}/reveal", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"wrong"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner can change the PIN")
    void changePin() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();
        String id = createCard(bearer, account.getId());

        mockMvc.perform(put("/api/cards/{id}/pin", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Passw0rd!23","newPin":"9876"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Reporting a card lost blocks further modification")
    void reportLostThenModifyIsConflict() throws Exception {
        BankAccount account = saveAccount(USER_ID, BigDecimal.valueOf(100));
        String bearer = userBearer();
        String id = createCard(bearer, account.getId());

        mockMvc.perform(post("/api/cards/{id}/report-lost", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST_STOLEN"));

        mockMvc.perform(post("/api/cards/{id}/report-lost", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/cards/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cardholderName":"New Name"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A user cannot touch a card on another user's account")
    void userCannotAccessOthersCard() throws Exception {
        BankAccount account = saveAccount(UUID.randomUUID(), BigDecimal.valueOf(100));
        String id = createCard(adminBearer(), account.getId());

        mockMvc.perform(get("/api/cards/{id}", id).header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Only an admin may list all cards")
    void listAllIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/cards").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/cards").header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }
}

package transactionService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import transactionService.demo.support.TransactionIntegrationTest;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CRUD + authorization contract of the plain (non-saga) transaction endpoints, driven as admin. */
class TransactionCrudTests extends TransactionIntegrationTest {

    private JsonNode createDeposit(String bearer) throws Exception {
        return json(mockMvc.perform(post("/api/transactions")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationAccountId":"%s","amount":250.00,"currency":"RON",
                                 "type":"DEPOSIT","description":"unit test deposit"}
                                """.formatted(ELENA_ACCOUNT)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    @Test
    @DisplayName("Admin can record a transaction, which defaults to PENDING and gets a saga id")
    void createDefaultsPendingWithSagaId() throws Exception {
        String bearer = adminBearer();

        JsonNode created = createDeposit(bearer);

        org.assertj.core.api.Assertions.assertThat(created.get("status").asString()).isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(created.get("sagaId").isNull()).isFalse();

        String id = created.get("id").asString();
        mockMvc.perform(get("/api/transactions/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("Admin can update a transaction's status and delete it")
    void updateAndDelete() throws Exception {
        String bearer = adminBearer();
        String id = createDeposit(bearer).get("id").asString();

        mockMvc.perform(put("/api/transactions/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED","description":"settled"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.description").value("settled"));

        mockMvc.perform(delete("/api/transactions/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A transaction referencing an unknown category is rejected with 404")
    void unknownCategoryIsNotFound() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","destinationAccountId":"%s","amount":10.00,
                                 "currency":"RON","type":"DEPOSIT"}
                                """.formatted(UUID.randomUUID(), ELENA_ACCOUNT)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A zero amount is rejected with 400")
    void zeroAmountIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationAccountId":"%s","amount":0.00,"currency":"RON","type":"DEPOSIT"}
                                """.formatted(ELENA_ACCOUNT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can list all transactions and see the seeded rows")
    void adminListsAll() throws Exception {
        mockMvc.perform(get("/api/transactions?page=0&size=20").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("A non-admin cannot list all transactions")
    void userCannotListAll() throws Exception {
        mockMvc.perform(get("/api/transactions").header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can list the transactions produced by a schedule")
    void listBySchedule() throws Exception {
        mockMvc.perform(get("/api/scheduled-transactions/{id}/transactions", SEEDED_SCHEDULED_ID)
                        .header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scheduledTransactionId").value(SEEDED_SCHEDULED_ID));
    }

    @Test
    @DisplayName("GET /api/transactions/me returns empty when bankingService cannot be reached")
    void listMineFailsClosedToEmpty() throws Exception {
        mockMvc.perform(get("/api/transactions/me").header("Authorization", userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("Fetching an unknown transaction returns 404")
    void unknownTransactionIsNotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/{id}", UUID.randomUUID()).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }
}

package transactionService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import transactionService.demo.support.TransactionIntegrationTest;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full CRUD + authorization contract of {@code /api/scheduled-transactions}, driven as admin. */
class ScheduledTransactionCrudTests extends TransactionIntegrationTest {

    private final String future = LocalDate.now().plusMonths(1).toString();

    private JsonNode createSchedule(String bearer) throws Exception {
        return json(mockMvc.perform(post("/api/scheduled-transactions")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":75.00,
                                 "currency":"RON","frequency":"WEEKLY","nextExecutionDate":"%s",
                                 "description":"weekly savings"}
                                """.formatted(ELENA_ACCOUNT, MIHAI_ACCOUNT, future)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    @Test
    @DisplayName("Admin can create, fetch, update and delete a schedule")
    void fullLifecycle() throws Exception {
        String bearer = adminBearer();

        JsonNode created = createSchedule(bearer);
        org.assertj.core.api.Assertions.assertThat(created.get("status").asString()).isEqualTo("ACTIVE");
        String id = created.get("id").asString();

        mockMvc.perform(get("/api/scheduled-transactions/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));

        mockMvc.perform(put("/api/scheduled-transactions/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":120.00,"status":"PAUSED","frequency":"MONTHLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"));

        mockMvc.perform(delete("/api/scheduled-transactions/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/scheduled-transactions/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A schedule referencing a known category is accepted")
    void createWithCategory() throws Exception {
        String bearer = adminBearer();
        String rentId = categoryId(bearer, "RENT");

        mockMvc.perform(post("/api/scheduled-transactions")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","sourceAccountId":"%s","destinationAccountId":"%s",
                                 "amount":30.00,"currency":"RON","frequency":"DAILY","nextExecutionDate":"%s"}
                                """.formatted(rentId, ELENA_ACCOUNT, MIHAI_ACCOUNT, future)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(rentId));
    }

    @Test
    @DisplayName("A schedule referencing an unknown category is rejected with 404")
    void unknownCategoryIsNotFound() throws Exception {
        mockMvc.perform(post("/api/scheduled-transactions")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","sourceAccountId":"%s","destinationAccountId":"%s",
                                 "amount":30.00,"currency":"RON","frequency":"DAILY","nextExecutionDate":"%s"}
                                """.formatted(UUID.randomUUID(), ELENA_ACCOUNT, MIHAI_ACCOUNT, future)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A past execution date is rejected with 400")
    void pastDateIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/scheduled-transactions")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":30.00,
                                 "currency":"RON","frequency":"DAILY","nextExecutionDate":"2000-01-01"}
                                """.formatted(ELENA_ACCOUNT, MIHAI_ACCOUNT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can list schedules and see the seeded row")
    void adminListsAll() throws Exception {
        mockMvc.perform(get("/api/scheduled-transactions?page=0&size=20").header("Authorization", adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("A non-admin cannot list all schedules")
    void userCannotListAll() throws Exception {
        mockMvc.perform(get("/api/scheduled-transactions").header("Authorization", userBearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Fetching an unknown schedule returns 404")
    void unknownScheduleIsNotFound() throws Exception {
        mockMvc.perform(get("/api/scheduled-transactions/{id}", UUID.randomUUID())
                        .header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }
}

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

/** Full CRUD + authorization contract of {@code /api/categories}. */
class CategoryCrudTests extends TransactionIntegrationTest {

    private String createCategory(String bearer, String name) throws Exception {
        JsonNode created = json(mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"created by test"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn());
        return created.get("id").asString();
    }

    @Test
    @DisplayName("Admin can create, fetch, update and delete a category")
    void fullLifecycle() throws Exception {
        String bearer = adminBearer();
        String name = "CAT_" + UUID.randomUUID().toString().substring(0, 8);

        String id = createCategory(bearer, name);

        mockMvc.perform(get("/api/categories/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        mockMvc.perform(put("/api/categories/{id}", id)
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("updated"));

        mockMvc.perform(delete("/api/categories/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Any authenticated user can read categories")
    void userCanListCategories() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("A non-admin cannot create a category")
    void userCannotCreate() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", userBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"USER_MADE"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A duplicate category name is rejected with 409")
    void duplicateNameIsConflict() throws Exception {
        String bearer = adminBearer();
        String name = "DUP_" + UUID.randomUUID().toString().substring(0, 8);
        createCategory(bearer, name);

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A blank name is rejected with 400")
    void blankNameIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", adminBearer()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deleting a category still referenced by transactions is rejected with 409")
    void deleteReferencedIsConflict() throws Exception {
        String bearer = adminBearer();
        String rentId = categoryId(bearer, "RENT");

        mockMvc.perform(delete("/api/categories/{id}", rentId).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Fetching an unknown category returns 404")
    void unknownCategoryIsNotFound() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", UUID.randomUUID()).header("Authorization", adminBearer()))
                .andExpect(status().isNotFound());
    }
}

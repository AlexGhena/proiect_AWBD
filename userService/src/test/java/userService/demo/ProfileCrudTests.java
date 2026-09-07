package userService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import userService.demo.support.UserServiceIntegrationTest;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full CRUD + authorization contract of {@code /api/profiles}. */
class ProfileCrudTests extends UserServiceIntegrationTest {

    /** Creates a fresh user (no profile yet) and returns its id. */
    private String createUser(String bearer) throws Exception {
        String username = "prof.user." + UUID.randomUUID().toString().substring(0, 8);
        JsonNode user = json(mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"Passw0rd!23"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn());
        return user.get("id").asString();
    }

    @Test
    @DisplayName("Admin can create, fetch, update and delete a profile")
    void fullLifecycle() throws Exception {
        String bearer = adminBearer();
        String userId = createUser(bearer);

        JsonNode created = json(mockMvc.perform(post("/api/profiles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","firstName":"Ana","lastName":"Pop","phone":"+40700000000"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andReturn());
        String id = created.get("id").asString();

        mockMvc.perform(get("/api/profiles/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Pop"));

        mockMvc.perform(get("/api/users/{userId}/profile", userId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/api/profiles/{id}", id)
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana-Maria"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana-Maria"))
                .andExpect(jsonPath("$.lastName").value("Pop"));

        mockMvc.perform(delete("/api/profiles/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profiles/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin can list profiles")
    void listProfiles() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/profiles?page=0&size=20").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Creating a second profile for a user that already has one is rejected with 409")
    void duplicateProfileIsConflict() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/profiles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","firstName":"Dup","lastName":"Licate"}
                                """.formatted(ELENA_USER_ID)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Creating a profile for an unknown user returns 404")
    void unknownUserIsNotFound() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/profiles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","firstName":"No","lastName":"User"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A blank first name is rejected with 400")
    void blankFirstNameIsBadRequest() throws Exception {
        String bearer = adminBearer();
        String userId = createUser(bearer);

        mockMvc.perform(post("/api/profiles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","firstName":"","lastName":"Pop"}
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A user may read their own profile but not another user's")
    void ownershipOnGetProfile() throws Exception {
        String userBearer = userBearer();

        mockMvc.perform(get("/api/profiles/{id}", ELENA_PROFILE_ID).header("Authorization", userBearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profiles/{id}", MIHAI_PROFILE_ID).header("Authorization", userBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A non-admin user cannot list all profiles")
    void listProfilesForbiddenForUser() throws Exception {
        String bearer = userBearer();

        mockMvc.perform(get("/api/profiles").header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }
}

package userService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import userService.demo.support.UserServiceIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full CRUD + authorization contract of {@code /api/roles} and user-role assignment. */
class RoleCrudTests extends UserServiceIntegrationTest {

    private String createRole(String bearer, String name) throws Exception {
        JsonNode created = json(mockMvc.perform(post("/api/roles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","description":"created by test"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn());
        return created.get("id").asString();
    }

    @Test
    @DisplayName("Admin can create, fetch, update and delete a role")
    void fullLifecycle() throws Exception {
        String bearer = adminBearer();
        String name = "ROLE_TEST_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String id = createRole(bearer, name);

        mockMvc.perform(get("/api/roles/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        mockMvc.perform(put("/api/roles/{id}", id)
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("updated"));

        mockMvc.perform(delete("/api/roles/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/roles/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Listing roles returns the seeded roles")
    void listRoles() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/roles?page=0&size=50").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Creating a role with an existing name is rejected with 409")
    void duplicateNameIsConflict() throws Exception {
        String bearer = adminBearer();
        String name = "ROLE_DUP_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        createRole(bearer, name);

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("An invalid role name shape is rejected with 400")
    void invalidNameIsBadRequest() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"not-a-role"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Fetching a non-existent role returns 404")
    void unknownRoleIsNotFound() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/roles/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deleting a role still assigned to a user is rejected with 409")
    void deleteReferencedRoleIsConflict() throws Exception {
        String bearer = adminBearer();

        JsonNode roles = json(mockMvc.perform(get("/api/roles?page=0&size=50").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn());
        String userRoleId = null;
        for (JsonNode role : roles.get("content")) {
            if ("ROLE_USER".equals(role.get("name").asString())) {
                userRoleId = role.get("id").asString();
            }
        }
        assertThat(userRoleId).isNotNull();

        mockMvc.perform(delete("/api/roles/{id}", userRoleId).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A non-admin user is forbidden from the roles API")
    void nonAdminForbidden() throws Exception {
        String bearer = userBearer();

        mockMvc.perform(get("/api/roles").header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated access to the roles API is rejected with 401")
    void anonymousUnauthorized() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin can assign and unassign a role to a user")
    void assignAndUnassignRole() throws Exception {
        String bearer = adminBearer();
        String name = "ROLE_ASSIGN_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String roleId = createRole(bearer, name);

        mockMvc.perform(post("/api/users/{userId}/roles/{roleId}", ELENA_USER_ID, roleId)
                        .header("Authorization", bearer).with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/{userId}/roles", ELENA_USER_ID).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + name + "')]").exists());

        mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", ELENA_USER_ID, roleId)
                        .header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Assigning the same role twice is rejected with 409")
    void doubleAssignIsConflict() throws Exception {
        String bearer = adminBearer();
        String name = "ROLE_TWICE_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String roleId = createRole(bearer, name);

        mockMvc.perform(post("/api/users/{userId}/roles/{roleId}", ELENA_USER_ID, roleId)
                        .header("Authorization", bearer).with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users/{userId}/roles/{roleId}", ELENA_USER_ID, roleId)
                        .header("Authorization", bearer).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Assigning a role to an unknown user returns 404")
    void assignToUnknownUserIsNotFound() throws Exception {
        String bearer = adminBearer();
        String name = "ROLE_NOUSER_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String roleId = createRole(bearer, name);

        mockMvc.perform(post("/api/users/{userId}/roles/{roleId}", UUID.randomUUID(), roleId)
                        .header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNotFound());
    }
}

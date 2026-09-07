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

/** Full CRUD + authorization contract of the address endpoints. */
class AddressCrudTests extends UserServiceIntegrationTest {

    private String createAddress(String bearer, String profileId) throws Exception {
        JsonNode created = json(mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","label":"WORK","street":"Str. Testului 1",
                                 "city":"Bucuresti","postalCode":"010101","country":"RO","isDefault":false}
                                """.formatted(profileId)))
                .andExpect(status().isCreated())
                .andReturn());
        return created.get("id").asString();
    }

    @Test
    @DisplayName("Admin can create, fetch, update and delete an address")
    void fullLifecycle() throws Exception {
        String bearer = adminBearer();

        String id = createAddress(bearer, MIHAI_PROFILE_ID);

        mockMvc.perform(get("/api/addresses/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Bucuresti"));

        mockMvc.perform(put("/api/addresses/{id}", id)
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city":"Cluj-Napoca","label":"HOME"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Cluj-Napoca"))
                .andExpect(jsonPath("$.label").value("HOME"));

        mockMvc.perform(delete("/api/addresses/{id}", id).header("Authorization", bearer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/addresses/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Label defaults to HOME when omitted")
    void labelDefaultsToHome() throws Exception {
        String bearer = adminBearer();

        JsonNode created = json(mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","street":"Str. Fara Label 2","city":"Iasi",
                                 "postalCode":"700100","country":"RO"}
                                """.formatted(MIHAI_PROFILE_ID)))
                .andExpect(status().isCreated())
                .andReturn());

        org.assertj.core.api.Assertions.assertThat(created.get("label").asString()).isEqualTo("HOME");
        org.assertj.core.api.Assertions.assertThat(created.get("isDefault").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Listing addresses by profile returns the seeded rows")
    void listByProfile() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/profiles/{profileId}/addresses", ELENA_PROFILE_ID)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("Admin can list all addresses")
    void listAll() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/addresses?page=0&size=20").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Creating an address for an unknown profile returns 404")
    void unknownProfileIsNotFound() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","street":"Nowhere 1","city":"Nulltown",
                                 "postalCode":"000000","country":"RO"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A non ISO country code is rejected with 400")
    void invalidCountryIsBadRequest() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","street":"Str. Rea 3","city":"Bucuresti",
                                 "postalCode":"010101","country":"Romania"}
                                """.formatted(MIHAI_PROFILE_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Marking a second address as default for a profile is rejected with 409")
    void secondDefaultIsConflict() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", bearer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","label":"WORK","street":"Str. Second Default 9",
                                 "city":"Bucuresti","postalCode":"010101","country":"RO","isDefault":true}
                                """.formatted(ELENA_PROFILE_ID)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Fetching an unknown address returns 404")
    void unknownAddressIsNotFound() throws Exception {
        String bearer = adminBearer();

        mockMvc.perform(get("/api/addresses/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A user can read an address on their own profile but not on another's")
    void ownershipOnGetAddress() throws Exception {
        String userBearer = userBearer();

        mockMvc.perform(get("/api/addresses/{id}", ELENA_ADDRESS_ID).header("Authorization", userBearer))
                .andExpect(status().isOk());

        String otherAddressId = createAddress(adminBearer(), MIHAI_PROFILE_ID);
        mockMvc.perform(get("/api/addresses/{id}", otherAddressId).header("Authorization", userBearer))
                .andExpect(status().isForbidden());
    }
}

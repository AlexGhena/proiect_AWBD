package userService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import userService.demo.support.TestRsaKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pagination and sorting contract of {@code GET /api/users}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPaginationTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();
    private static final String ADMIN_NAME = "cristina.ionescu";
    private static final String ADMIN_PASSWORD = "AdminP@ss25";

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key", KEYS::privateKeyPem);
        registry.add("app.security.jwt.public-key", KEYS::publicKeyPem);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminBearer() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
        return "Bearer " + body.get("accessToken").asString();
    }

    /** Creates a handful of admin-visible fixture users with a shared, distinctive prefix. */
    private List<String> createFixtureUsers(String bearer, String prefix, int count) throws Exception {
        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String username = prefix + i + "." + UUID.randomUUID().toString().substring(0, 6);
            usernames.add(username);
            mockMvc.perform(post("/api/users")
                            .header("Authorization", bearer)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","email":"%s@example.com","password":"Passw0rd!23"}
                                    """.formatted(username, username)))
                    .andExpect(status().isCreated());
        }
        return usernames;
    }

    private JsonNode list(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users" + query).header("Authorization", bearer))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode listExpectingBadRequest(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users" + query).header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    private static List<String> usernamesOf(JsonNode content) {
        List<String> result = new ArrayList<>();
        content.forEach(node -> result.add(node.get("username").asString()));
        return result;
    }

    @Test
    @DisplayName("The first page reports page=0 and first=true")
    void firstPage() throws Exception {
        String bearer = adminBearer();
        createFixtureUsers(bearer, "pg.first.", 6);

        JsonNode body = list(bearer, "?page=0&size=5&sortBy=username&sortDirection=asc");

        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("first").asBoolean()).isTrue();
        assertThat(body.get("content")).hasSize(5);
    }

    @Test
    @DisplayName("Requesting the next page advances past the first page's rows")
    void nextPage() throws Exception {
        String bearer = adminBearer();
        List<String> created = createFixtureUsers(bearer, "pg.next.", 6);

        JsonNode firstPage = list(bearer, "?page=0&size=5&sortBy=username&sortDirection=asc");
        JsonNode secondPage = list(bearer, "?page=1&size=5&sortBy=username&sortDirection=asc");

        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
        assertThat(secondPage.get("first").asBoolean()).isFalse();

        List<String> firstUsernames = usernamesOf(firstPage.get("content"));
        List<String> secondUsernames = usernamesOf(secondPage.get("content"));
        assertThat(firstUsernames).doesNotContainAnyElementsOf(secondUsernames);
        // Sanity check that our fixtures were actually spread across both pages.
        assertThat(firstUsernames.size() + secondUsernames.size()).isGreaterThanOrEqualTo(created.size());
    }

    @Test
    @DisplayName("The size parameter controls how many rows come back")
    void pageSize() throws Exception {
        String bearer = adminBearer();
        createFixtureUsers(bearer, "pg.size.", 3);

        JsonNode body = list(bearer, "?page=0&size=2");

        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("content")).hasSize(2);
    }

    @Test
    @DisplayName("sortDirection=asc orders the fixture rows from smallest to largest")
    void sortAscending() throws Exception {
        String bearer = adminBearer();
        List<String> created = createFixtureUsers(bearer, "pg.asc.", 4);

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=username&sortDirection=asc");
        List<String> ordered = usernamesOf(body.get("content")).stream()
                .filter(created::contains)
                .toList();

        assertThat(ordered).isSorted();
        assertThat(ordered).hasSize(created.size());
    }

    @Test
    @DisplayName("sortDirection=desc orders the fixture rows from largest to smallest")
    void sortDescending() throws Exception {
        String bearer = adminBearer();
        List<String> created = createFixtureUsers(bearer, "pg.desc.", 4);

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=username&sortDirection=desc");
        List<String> ordered = usernamesOf(body.get("content")).stream()
                .filter(created::contains)
                .toList();

        assertThat(ordered).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(ordered).hasSize(created.size());
    }

    @Test
    @DisplayName("An unknown sortBy field is rejected with 400")
    void invalidSortFieldIsRejected() throws Exception {
        String bearer = adminBearer();

        JsonNode problem = listExpectingBadRequest(bearer, "?sortBy=passwordHash");

        assertThat(problem.get("status").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("A size above the configured maximum is rejected with 400")
    void sizeAboveMaximumIsRejected() throws Exception {
        String bearer = adminBearer();

        listExpectingBadRequest(bearer, "?size=1000");
    }

    @Test
    @DisplayName("A page far beyond the data returns an empty content list")
    void pageWithNoResults() throws Exception {
        String bearer = adminBearer();

        JsonNode body = list(bearer, "?page=9999&size=20");

        assertThat(body.get("content")).isEmpty();
        assertThat(body.get("last").asBoolean()).isTrue();
    }
}

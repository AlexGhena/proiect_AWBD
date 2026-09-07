package userService.demo.support;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared wiring for full-stack userService integration tests: boots the app on the {@code test}
 * profile (H2), rolls each test back, and exposes helpers for logging seeded users in and reading
 * JSON responses. The seed data lives in {@code V2__seed_test_data.sql}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class UserServiceIntegrationTest {

    protected static final TestRsaKeys KEYS = TestRsaKeys.generate();

    protected static final String ADMIN_USERNAME = "cristina.ionescu";
    protected static final String ADMIN_PASSWORD = "AdminP@ss25";
    protected static final String USER_USERNAME = "elena.dumitrescu";
    protected static final String USER_PASSWORD = "Passw0rd!23";

    /** Seeded ids from V2__seed_test_data.sql. */
    protected static final String ELENA_USER_ID = "12d9ff81-1d75-4c56-acdd-3597207412bc";
    protected static final String ELENA_PROFILE_ID = "954b306e-b7d0-42a0-973f-ce75c662307d";
    protected static final String ELENA_ADDRESS_ID = "89a9574c-619a-4cd0-b345-c5b50489fe66";
    protected static final String MIHAI_PROFILE_ID = "5b24d9e1-d6d2-490e-a762-2ea0c84c69d7";

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key", KEYS::privateKeyPem);
        registry.add("app.security.jwt.public-key", KEYS::publicKeyPem);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminBearer() throws Exception {
        return bearerFor(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected String userBearer() throws Exception {
        return bearerFor(USER_USERNAME, USER_PASSWORD);
    }

    protected String bearerFor(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + json(login).get("accessToken").asString();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }
}

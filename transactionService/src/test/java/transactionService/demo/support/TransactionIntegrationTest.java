package transactionService.demo.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Shared wiring for full-stack transactionService integration tests. Uses the in-memory
 * {@link TestJwtDecoderConfig} so tokens validate without a live userService; the bankingService URL
 * stays unreachable on the {@code test} profile, so ownership checks that need it fail closed - hence
 * the CRUD tests here drive the admin path, which does not depend on that hop. Rolls each test back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
public abstract class TransactionIntegrationTest {

    protected static final UUID ADMIN_ID = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");
    protected static final UUID USER_ID = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");

    protected static final String SEEDED_SCHEDULED_ID = "9ff480f0-c79b-44de-8928-476858d2a66a";
    protected static final String SEEDED_TRANSACTION_ID = "778eebf6-ca92-4682-ae6e-7c4c743d4171";
    protected static final String ELENA_ACCOUNT = "3255df08-8624-499c-8ab1-12f721653327";
    protected static final String MIHAI_ACCOUNT = "a0f3e83a-9bc2-4271-b67b-7c92580f1790";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestTokens tokens;

    protected String adminBearer() {
        return "Bearer " + tokens.validToken(ADMIN_ID, "cristina.ionescu", "ROLE_ADMIN");
    }

    protected String userBearer() {
        return "Bearer " + tokens.validToken(USER_ID, "elena.dumitrescu", "ROLE_USER");
    }

    protected JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }

    /** Fetches a seeded category id by name. */
    protected String categoryId(String bearer, String name) throws Exception {
        JsonNode page = json(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/categories?page=0&size=100").header("Authorization", bearer))
                .andReturn());
        for (JsonNode c : page.get("content")) {
            if (name.equals(c.get("name").asString())) {
                return c.get("id").asString();
            }
        }
        throw new IllegalStateException("Seeded category not found: " + name);
    }
}

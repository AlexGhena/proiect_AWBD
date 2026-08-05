package transactionService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import transactionService.demo.support.TestJwtDecoderConfig;
import transactionService.demo.support.TestTokens;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * transactionService validates tokens independently and applies its own role rules.
 *
 * <p>The configured bankingService URL is unreachable in this profile, which is deliberate: it lets
 * the suite assert that an ownership check which cannot be completed denies rather than allows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class TransactionSecurityTests {

    private static final UUID ELENA = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");
    private static final UUID ADMIN = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");
    private static final String SEEDED_TRANSACTION = "778eebf6-ca92-4682-ae6e-7c4c743d4171";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTokens tokens;

    private String userBearer() {
        return "Bearer " + tokens.validToken(ELENA, "elena.dumitrescu", "ROLE_USER");
    }

    private String adminBearer() {
        return "Bearer " + tokens.validToken(ADMIN, "cristina.ionescu", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("A request with no token is refused")
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired, wrong-issuer and wrong-audience tokens are all refused")
    void invalidTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + tokens.expiredToken(ELENA, "ROLE_USER")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + tokens.tokenWithIssuer("evilIssuer", ELENA, "ROLE_USER")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + tokens.tokenWithAudience("other-api", ELENA, "ROLE_USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token signed by a foreign key is refused")
    void foreignSignatureIsRejected() throws Exception {
        TestTokens attacker = TestTokens.generate();
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer " + attacker.validToken(ELENA, "elena", "ROLE_ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Categories are readable by any authenticated user but writable only by an admin")
    void categoryRulesSeparateReadFromWrite() throws Exception {
        mockMvc.perform(get("/api/categories").header("Authorization", userBearer()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .header("Authorization", userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"SNEAKY","description":"should not be created"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listing every transaction is reserved for an administrator")
    void listingAllTransactionsRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/transactions").header("Authorization", userBearer()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions").header("Authorization", adminBearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("An ownership check that cannot reach bankingService denies rather than allows")
    void unreachableOwnershipCheckFailsClosed() throws Exception {
        // bankingService is unreachable in this profile, so the cross-service check cannot succeed.
        mockMvc.perform(get("/api/transactions/" + SEEDED_TRANSACTION)
                        .header("Authorization", userBearer()))
                .andExpect(status().isForbidden());

        // An administrator does not depend on the cross-service check at all.
        mockMvc.perform(get("/api/transactions/" + SEEDED_TRANSACTION)
                        .header("Authorization", adminBearer()))
                .andExpect(status().isOk());
    }
}

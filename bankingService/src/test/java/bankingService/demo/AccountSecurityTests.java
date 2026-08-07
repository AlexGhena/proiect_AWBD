package bankingService.demo;

import bankingService.demo.support.TestJwtDecoderConfig;
import bankingService.demo.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * bankingService must decide authorization for itself, trusting only a token it has validated.
 *
 * <p>Account ids and owners come from the seeded migration data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class AccountSecurityTests {

    private static final UUID ELENA = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");
    private static final UUID MIHAI = UUID.fromString("af944ee3-769a-4c79-b946-235768c1b3bf");
    private static final UUID ADMIN = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");

    private static final String ELENA_ACCOUNT = "3255df08-8624-499c-8ab1-12f721653327";
    private static final String MIHAI_ACCOUNT = "a0f3e83a-9bc2-4271-b67b-7c92580f1790";
    private static final String ELENA_CARD = "8d7d638c-3fa0-42b3-b1da-8302a4ea7eb3";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTokens tokens;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String userToken(UUID userId) {
        return tokens.validToken(userId, "test.user", "ROLE_USER");
    }

    private String adminToken() {
        return tokens.validToken(ADMIN, "cristina.ionescu", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("A request with no token is refused")
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cards")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An expired token is refused")
    void expiredTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)
                        .header("Authorization", bearer(tokens.expiredToken(ELENA, "ROLE_USER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token from the wrong issuer is refused")
    void wrongIssuerIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)
                        .header("Authorization", bearer(tokens.tokenWithIssuer("evilIssuer", ELENA, "ROLE_USER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token minted for another audience is refused")
    void wrongAudienceIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)
                        .header("Authorization", bearer(tokens.tokenWithAudience("some-other-api", ELENA, "ROLE_USER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A token signed with a different key is refused")
    void wrongSignatureIsRejected() throws Exception {
        // A separate key pair stands in for an attacker forging their own token.
        TestTokens attacker = TestTokens.generate();
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)
                        .header("Authorization", bearer(attacker.validToken(ELENA, "elena", "ROLE_ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A malformed token is refused")
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listing every account is reserved for an administrator")
    void listingAllAccountsRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", bearer(userToken(ELENA))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A user reaches their own account but not somebody else's")
    void accountOwnershipIsEnforced() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ELENA_ACCOUNT)
                        .header("Authorization", bearer(userToken(ELENA))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + MIHAI_ACCOUNT)
                        .header("Authorization", bearer(userToken(ELENA))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An administrator reaches any account")
    void adminReachesAnyAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/" + MIHAI_ACCOUNT)
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Card ownership follows the account the card belongs to")
    void cardOwnershipFollowsAccount() throws Exception {
        mockMvc.perform(get("/api/cards/" + ELENA_CARD)
                        .header("Authorization", bearer(userToken(ELENA))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cards/" + ELENA_CARD)
                        .header("Authorization", bearer(userToken(MIHAI))))
                .andExpect(status().isForbidden());
    }
}

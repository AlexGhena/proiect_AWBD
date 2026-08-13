package bankingService.demo;

import bankingService.demo.security.UserServiceClient;
import bankingService.demo.support.TestJwtDecoderConfig;
import bankingService.demo.support.TestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Show details / show PIN / change PIN / report lost-stolen. userService is never actually called -
 * {@link UserServiceClient} is stubbed, the same way {@code RegistrationApprovalTests} stubs
 * userService's own outbound client to bankingService.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class CardSecretsTests {

    // Seeded by V2__seed_test_data.sql - a legacy card with no cardNumber/cvv/pin, exercising the
    // lazy-backfill path.
    private static final UUID ELENA = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");
    private static final UUID MIHAI = UUID.fromString("af944ee3-769a-4c79-b946-235768c1b3bf");
    private static final UUID ELENA_CARD = UUID.fromString("8d7d638c-3fa0-42b3-b1da-8302a4ea7eb3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTokens tokens;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private String elenaBearer() {
        return "Bearer " + tokens.validToken(ELENA, "elena.dumitrescu", "ROLE_USER");
    }

    private String mihaiBearer() {
        return "Bearer " + tokens.validToken(MIHAI, "mihai.radulescu", "ROLE_USER");
    }

    @Test
    @DisplayName("Reveal succeeds with the right password and backfills a legacy card's PAN/CVV")
    void revealSucceedsAndBackfillsLegacyCard() throws Exception {
        when(userServiceClient.verifyPassword(eq(ELENA), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/reveal")
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Passw0rd!23"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(org.hamcrest.Matchers.endsWith("7421")))
                .andExpect(jsonPath("$.cardNumber", org.hamcrest.Matchers.hasLength(16)))
                .andExpect(jsonPath("$.cvv").isNotEmpty())
                .andExpect(jsonPath("$.cardholderName").value("Elena Dumitrescu"));
    }

    @Test
    @DisplayName("Reveal is rejected with 403 when the password does not verify")
    void revealFailsOnWrongPassword() throws Exception {
        when(userServiceClient.verifyPassword(eq(ELENA), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/reveal")
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"wrong"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A caller who does not own the card is denied before password verification even runs")
    void nonOwnerCannotReveal() throws Exception {
        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/reveal")
                        .header("Authorization", mihaiBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"irrelevant"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Changing the PIN takes effect immediately, visible on the next reveal")
    void changePinTakesEffect() throws Exception {
        when(userServiceClient.verifyPassword(eq(ELENA), any())).thenReturn(true);

        mockMvc.perform(put("/api/cards/" + ELENA_CARD + "/pin")
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Passw0rd!23","newPin":"9182"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/pin/reveal")
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"Passw0rd!23"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pin").value("9182"));
    }

    @Test
    @DisplayName("Changing the PIN to a non-4-digit value is rejected with 400")
    void changePinRejectsBadFormat() throws Exception {
        mockMvc.perform(put("/api/cards/" + ELENA_CARD + "/pin")
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Passw0rd!23","newPin":"12"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reporting a card lost/stolen is terminal - the card can no longer be updated")
    void reportLostIsTerminal() throws Exception {
        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/report-lost")
                        .header("Authorization", elenaBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST_STOLEN"));

        mockMvc.perform(put("/api/cards/" + ELENA_CARD)
                        .header("Authorization", elenaBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BLOCKED"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/cards/" + ELENA_CARD + "/report-lost")
                        .header("Authorization", elenaBearer()))
                .andExpect(status().isConflict());
    }
}

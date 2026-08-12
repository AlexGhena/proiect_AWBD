package bankingService.demo;

import bankingService.demo.domain.port.out.AccountRepositoryPort;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code POST /internal/accounts/provision} - opens an account with a freshly generated IBAN. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class AccountProvisioningTests {

    private static final UUID CALLER = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokens tokens;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    private String bearer() {
        return "Bearer " + tokens.validToken(CALLER, "test.user", "ROLE_USER");
    }

    @Test
    @DisplayName("Provisioning opens an ACTIVE, zero-balance account with a generated RO IBAN")
    void provisioningOpensAccountWithGeneratedIban() throws Exception {
        UUID userId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/internal/accounts/provision")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","currency":"RON"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("userId").asString()).isEqualTo(userId.toString());
        assertThat(body.get("iban").asString()).startsWith("RO").hasSize(24);
        assertThat(body.get("currency").asString()).isEqualTo("RON");
        assertThat(body.get("balance").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(body.get("status").asString()).isEqualTo("ACTIVE");

        assertThat(accountRepositoryPort.existsByIban(body.get("iban").asString())).isTrue();
    }

    @Test
    @DisplayName("Currency defaults to RON when omitted")
    void currencyDefaultsToRon() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/internal/accounts/provision")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s"}
                                """.formatted(userId)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A request with no token is refused")
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(post("/internal/accounts/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","currency":"RON"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}

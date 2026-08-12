package bankingService.demo;

import bankingService.demo.domain.model.AccountStatus;
import bankingService.demo.domain.model.BankAccount;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code /internal/accounts/**} - the balance operations the transfer Saga in transactionService
 * relies on. Reached only over the internal network, so authorization here is "any authenticated
 * caller", not resource ownership - covers debit/credit/compensate/idempotency behavior plus the
 * auth and header requirements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class InternalAccountOperationsTests {

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

    /** Saved directly through the repository port, bypassing the userService existence check. */
    private UUID fixtureAccount(String currency, BigDecimal balance, AccountStatus status) {
        String iban = ("TEST" + UUID.randomUUID()).toUpperCase().replaceAll("[^A-Z0-9]", "");
        BankAccount saved = accountRepositoryPort.save(BankAccount.builder()
                .userId(UUID.randomUUID())
                .iban(iban.substring(0, Math.min(iban.length(), 30)))
                .currency(currency)
                .balance(balance)
                .status(status)
                .build());
        return saved.getId();
    }

    private JsonNode postWithIdempotencyKey(String path, String idempotencyKey, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Debiting reduces the balance and returns the new balance")
    void debitReducesBalance() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();

        JsonNode response = postWithIdempotencyKey("/internal/accounts/" + accountId + "/debit", sagaId + ":DEBIT", """
                {"amount":30.00,"currency":"RON","sagaId":"%s"}
                """.formatted(sagaId));

        assertThat(new BigDecimal(response.get("balance").asString())).isEqualByComparingTo("70.00");
        assertThat(response.get("replayed").asBoolean()).isFalse();
        assertThat(accountRepositoryPort.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Crediting increases the balance")
    void creditIncreasesBalance() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();

        JsonNode response = postWithIdempotencyKey("/internal/accounts/" + accountId + "/credit", sagaId + ":CREDIT", """
                {"amount":45.50,"currency":"RON","sagaId":"%s"}
                """.formatted(sagaId));

        assertThat(new BigDecimal(response.get("balance").asString())).isEqualByComparingTo("145.50");
        assertThat(accountRepositoryPort.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("145.50");
    }

    @Test
    @DisplayName("A debit is applied only once even if the same Idempotency-Key is replayed")
    void debitIsIdempotent() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();
        String key = sagaId + ":DEBIT";
        String body = """
                {"amount":30.00,"currency":"RON","sagaId":"%s"}
                """.formatted(sagaId);

        JsonNode first = postWithIdempotencyKey("/internal/accounts/" + accountId + "/debit", key, body);
        JsonNode second = postWithIdempotencyKey("/internal/accounts/" + accountId + "/debit", key, body);

        assertThat(first.get("replayed").asBoolean()).isFalse();
        assertThat(second.get("replayed").asBoolean()).isTrue();
        assertThat(new BigDecimal(second.get("balance").asString())).isEqualByComparingTo("70.00");
        assertThat(accountRepositoryPort.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("A debit larger than the balance is rejected with 409 and moves no money")
    void debitFailsOnInsufficientFunds() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("10.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/accounts/" + accountId + "/debit")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", sagaId + ":DEBIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":50.00,"currency":"RON","sagaId":"%s"}
                                """.formatted(sagaId)))
                .andExpect(status().isConflict());

        assertThat(accountRepositoryPort.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("A debit against a blocked account is rejected with 409")
    void debitOnBlockedAccountFails() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.BLOCKED);
        String sagaId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/accounts/" + accountId + "/debit")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", sagaId + ":DEBIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"RON","sagaId":"%s"}
                                """.formatted(sagaId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A debit whose currency does not match the account is rejected with 409")
    void debitFailsOnCurrencyMismatch() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/accounts/" + accountId + "/debit")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", sagaId + ":DEBIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"EUR","sagaId":"%s"}
                                """.formatted(sagaId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Compensating reverses exactly the amount of the original debit")
    void compensateReversesDebit() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();
        String debitKey = sagaId + ":DEBIT";

        postWithIdempotencyKey("/internal/accounts/" + accountId + "/debit", debitKey, """
                {"amount":30.00,"currency":"RON","sagaId":"%s"}
                """.formatted(sagaId));

        JsonNode compensated = postWithIdempotencyKey("/internal/accounts/" + accountId + "/compensate", sagaId + ":COMPENSATE", """
                {"originalIdempotencyKey":"%s","sagaId":"%s"}
                """.formatted(debitKey, sagaId));

        assertThat(new BigDecimal(compensated.get("balance").asString())).isEqualByComparingTo("100.00");
        assertThat(accountRepositoryPort.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Compensating against a key with no matching debit fails with 404")
    void compensateWithUnknownOriginalKeyFails() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);
        String sagaId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/accounts/" + accountId + "/compensate")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", sagaId + ":COMPENSATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalIdempotencyKey":"no-such-key","sagaId":"%s"}
                                """.formatted(sagaId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A missing Idempotency-Key header is rejected with 400")
    void missingIdempotencyKeyHeaderIsRejected() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);

        mockMvc.perform(post("/internal/accounts/" + accountId + "/debit")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"RON","sagaId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A request with no token is refused")
    void missingTokenIsRejected() throws Exception {
        UUID accountId = fixtureAccount("RON", new BigDecimal("100.00"), AccountStatus.ACTIVE);

        mockMvc.perform(get("/internal/accounts/" + accountId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/accounts/" + accountId + "/debit")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":10.00,"currency":"RON","sagaId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The snapshot endpoint reports currency and status without requiring ownership")
    void snapshotReturnsAccountDetails() throws Exception {
        UUID accountId = fixtureAccount("EUR", new BigDecimal("50.00"), AccountStatus.ACTIVE);

        mockMvc.perform(get("/internal/accounts/" + accountId).header("Authorization", bearer()))
                .andExpect(status().isOk());
    }
}

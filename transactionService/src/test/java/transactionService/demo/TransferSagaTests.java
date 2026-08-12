package transactionService.demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import transactionService.demo.support.TestJwtDecoderConfig;
import transactionService.demo.support.TestTokens;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The transfer Saga end to end (docs/BACKEND_ARCHITECTURE.md section 9), with bankingService
 * stubbed via WireMock so each branch (success, a business rejection, a downstream failure
 * requiring compensation, and bankingService being entirely unreachable) is deterministic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class TransferSagaTests {

    private static final UUID ADMIN = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");

    private static WireMockServer bankingService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokens tokens;

    @DynamicPropertySource
    static void bankingServiceUrl(DynamicPropertyRegistry registry) {
        bankingService = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        bankingService.start();
        registry.add("app.clients.banking-service-url", bankingService::baseUrl);
    }

    @AfterAll
    static void stopServer() {
        bankingService.stop();
    }

    @AfterEach
    void resetStubs() {
        bankingService.resetAll();
    }

    private String adminBearer() {
        // ADMIN short-circuits the @PreAuthorize ownership check, so the test only needs to stub
        // the /internal/** calls the saga itself makes - not the /api/accounts ownership lookup.
        return "Bearer " + tokens.validToken(ADMIN, "cristina.ionescu", "ROLE_ADMIN");
    }

    private void stubSnapshot(UUID accountId, String currency, String status) {
        bankingService.stubFor(WireMock.get(WireMock.urlEqualTo("/internal/accounts/" + accountId))
                .willReturn(WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","currency":"%s","status":"%s"}
                                """.formatted(accountId, currency, status))));
    }

    private void stubOperation(String path, int status, String body) {
        bankingService.stubFor(WireMock.post(WireMock.urlEqualTo(path))
                .willReturn(WireMock.aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)));
    }

    private String operationResponseBody(UUID accountId, String balance) {
        return """
                {"accountId":"%s","balance":%s,"replayed":false}
                """.formatted(accountId, balance);
    }

    private MvcResult postTransfer(UUID source, UUID destination, String amount, String currency) throws Exception {
        return mockMvc.perform(post("/api/transactions/transfers")
                        .header("Authorization", adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"currency":"%s"}
                                """.formatted(source, destination, amount, currency)))
                .andReturn();
    }

    @Test
    @DisplayName("A fully successful transfer debits, credits, and completes")
    void successfulTransferCompletes() throws Exception {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        stubSnapshot(source, "RON", "ACTIVE");
        stubSnapshot(destination, "RON", "ACTIVE");
        stubOperation("/internal/accounts/" + source + "/debit", 200, operationResponseBody(source, "70.00"));
        stubOperation("/internal/accounts/" + destination + "/credit", 200, operationResponseBody(destination, "170.00"));

        MvcResult result = postTransfer(source, destination, "30.00", "RON");

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(body.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(body.get("sagaId").asString()).isNotBlank();

        bankingService.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/accounts/" + destination + "/credit")));
    }

    @Test
    @DisplayName("A credit failure after a successful debit compensates the debit and returns 503")
    void creditFailureTriggersCompensation() throws Exception {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        stubSnapshot(source, "RON", "ACTIVE");
        stubSnapshot(destination, "RON", "ACTIVE");
        stubOperation("/internal/accounts/" + source + "/debit", 200, operationResponseBody(source, "70.00"));
        stubOperation("/internal/accounts/" + destination + "/credit", 500, """
                {"title":"Internal Server Error","status":500,"detail":"boom"}
                """);
        stubOperation("/internal/accounts/" + source + "/compensate", 200, operationResponseBody(source, "100.00"));

        MvcResult result = postTransfer(source, destination, "30.00", "RON");

        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        bankingService.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/accounts/" + source + "/compensate")));
    }

    @Test
    @DisplayName("A debit rejected as a business failure never reaches credit or compensate")
    void debitBusinessFailureStopsTheSaga() throws Exception {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        stubSnapshot(source, "RON", "ACTIVE");
        stubSnapshot(destination, "RON", "ACTIVE");
        stubOperation("/internal/accounts/" + source + "/debit", 409, """
                {"title":"Conflict","status":409,"detail":"Account has insufficient funds"}
                """);

        MvcResult result = postTransfer(source, destination, "9999.00", "RON");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        bankingService.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/accounts/" + destination + "/credit")));
        bankingService.verify(0, WireMock.postRequestedFor(WireMock.urlEqualTo("/internal/accounts/" + source + "/compensate")));
    }

    @Test
    @DisplayName("bankingService being unreachable fails the saga with 503 after retries")
    void bankingServiceUnreachableFailsWith503() throws Exception {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();
        bankingService.stubFor(WireMock.get(WireMock.urlEqualTo("/internal/accounts/" + source))
                .willReturn(WireMock.aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        MvcResult result = postTransfer(source, destination, "30.00", "RON");

        assertThat(result.getResponse().getStatus()).isEqualTo(503);
    }
}

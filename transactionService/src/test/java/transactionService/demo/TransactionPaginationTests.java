package transactionService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import transactionService.demo.domain.model.BankTransaction;
import transactionService.demo.domain.model.TransactionStatus;
import transactionService.demo.domain.model.TransactionType;
import transactionService.demo.domain.port.out.BankTransactionRepositoryPort;
import transactionService.demo.support.TestJwtDecoderConfig;
import transactionService.demo.support.TestTokens;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pagination and sorting contract of {@code GET /api/transactions}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class TransactionPaginationTests {

    private static final UUID ADMIN = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokens tokens;

    @Autowired
    private BankTransactionRepositoryPort bankTransactionRepositoryPort;

    private String adminBearer() {
        return "Bearer " + tokens.validToken(ADMIN, "cristina.ionescu", "ROLE_ADMIN");
    }

    /** Saved directly through the repository port so each test controls its own amounts/statuses. */
    private void createFixtureTransactions(int count, BigDecimal startingAmount, TransactionStatus status) {
        BigDecimal amount = startingAmount;
        for (int i = 0; i < count; i++) {
            bankTransactionRepositoryPort.save(BankTransaction.builder()
                    .sourceAccountId(UUID.randomUUID())
                    .destinationAccountId(UUID.randomUUID())
                    .sagaId(UUID.randomUUID())
                    .amount(amount)
                    .currency("RON")
                    .type(TransactionType.TRANSFER)
                    .status(status)
                    .build());
            amount = amount.add(BigDecimal.ONE);
        }
    }

    private JsonNode list(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/transactions" + query).header("Authorization", bearer))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode listExpectingBadRequest(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/transactions" + query).header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    private static List<Double> amountsInRange(JsonNode content, double lowInclusive, double highExclusive) {
        List<Double> result = new ArrayList<>();
        content.forEach(node -> {
            double amount = node.get("amount").asDouble();
            if (amount >= lowInclusive && amount < highExclusive) {
                result.add(amount);
            }
        });
        return result;
    }

    @Test
    @DisplayName("The first page reports page=0 and first=true")
    void firstPage() throws Exception {
        String bearer = adminBearer();
        createFixtureTransactions(6, BigDecimal.valueOf(10_000), TransactionStatus.PENDING);

        JsonNode body = list(bearer, "?page=0&size=5&sortBy=amount&sortDirection=asc");

        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("first").asBoolean()).isTrue();
        assertThat(body.get("content")).hasSize(5);
    }

    @Test
    @DisplayName("Requesting the next page advances past the first page's rows")
    void nextPage() throws Exception {
        String bearer = adminBearer();
        createFixtureTransactions(6, BigDecimal.valueOf(20_000), TransactionStatus.PENDING);

        JsonNode firstPage = list(bearer, "?page=0&size=5&sortBy=amount&sortDirection=asc");
        JsonNode secondPage = list(bearer, "?page=1&size=5&sortBy=amount&sortDirection=asc");

        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
        assertThat(secondPage.get("first").asBoolean()).isFalse();

        List<Double> firstAmounts = amountsInRange(firstPage.get("content"), 0, Double.MAX_VALUE);
        List<Double> secondAmounts = amountsInRange(secondPage.get("content"), 0, Double.MAX_VALUE);
        assertThat(firstAmounts).doesNotContainAnyElementsOf(secondAmounts);
    }

    @Test
    @DisplayName("The size parameter controls how many rows come back")
    void pageSize() throws Exception {
        String bearer = adminBearer();
        createFixtureTransactions(3, BigDecimal.valueOf(30_000), TransactionStatus.PENDING);

        JsonNode body = list(bearer, "?page=0&size=2");

        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("content")).hasSize(2);
    }

    @Test
    @DisplayName("sortDirection=asc orders the fixture rows by amount ascending")
    void sortAscending() throws Exception {
        String bearer = adminBearer();
        createFixtureTransactions(4, BigDecimal.valueOf(40_000), TransactionStatus.PENDING);

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=amount&sortDirection=asc");
        List<Double> ordered = amountsInRange(body.get("content"), 40_000, 40_100);

        assertThat(ordered).isSorted();
        assertThat(ordered).hasSize(4);
    }

    @Test
    @DisplayName("sortDirection=desc orders the fixture rows by amount descending")
    void sortDescending() throws Exception {
        String bearer = adminBearer();
        createFixtureTransactions(4, BigDecimal.valueOf(50_000), TransactionStatus.PENDING);

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=amount&sortDirection=desc");
        List<Double> ordered = amountsInRange(body.get("content"), 50_000, 50_100);

        assertThat(ordered).isSortedAccordingTo((a, b) -> Double.compare(b, a));
        assertThat(ordered).hasSize(4);
    }

    @Test
    @DisplayName("An unknown sortBy field is rejected with 400")
    void invalidSortFieldIsRejected() throws Exception {
        String bearer = adminBearer();

        JsonNode problem = listExpectingBadRequest(bearer, "?sortBy=description");

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

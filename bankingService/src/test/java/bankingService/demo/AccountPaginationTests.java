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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pagination and sorting contract of {@code GET /api/accounts}. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class AccountPaginationTests {

    private static final UUID ADMIN = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokens tokens;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    private String adminBearer() {
        return "Bearer " + tokens.validToken(ADMIN, "cristina.ionescu", "ROLE_ADMIN");
    }

    /** Saved directly through the repository port to skip the userService existence check the API path requires. */
    private List<String> createFixtureAccounts(String prefix, int count, BigDecimal startingBalance) {
        List<String> ibans = new ArrayList<>();
        BigDecimal balance = startingBalance;
        for (int i = 0; i < count; i++) {
            String iban = ("TEST" + prefix + i + UUID.randomUUID()).replaceAll("[^A-Z0-9]", "").toUpperCase();
            iban = iban.substring(0, Math.min(iban.length(), 34));
            ibans.add(iban);
            accountRepositoryPort.save(BankAccount.builder()
                    .userId(UUID.randomUUID())
                    .iban(iban)
                    .currency("RON")
                    .balance(balance)
                    .status(AccountStatus.ACTIVE)
                    .build());
            balance = balance.add(BigDecimal.TEN);
        }
        return ibans;
    }

    private JsonNode list(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/accounts" + query).header("Authorization", bearer))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode listExpectingBadRequest(String bearer, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/accounts" + query).header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    private static List<String> ibansOf(JsonNode content) {
        List<String> result = new ArrayList<>();
        content.forEach(node -> result.add(node.get("iban").asString()));
        return result;
    }

    @Test
    @DisplayName("The first page reports page=0 and first=true")
    void firstPage() throws Exception {
        String bearer = adminBearer();
        createFixtureAccounts("first", 6, BigDecimal.valueOf(100));

        JsonNode body = list(bearer, "?page=0&size=5&sortBy=iban&sortDirection=asc");

        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("first").asBoolean()).isTrue();
        assertThat(body.get("content")).hasSize(5);
    }

    @Test
    @DisplayName("Requesting the next page advances past the first page's rows")
    void nextPage() throws Exception {
        String bearer = adminBearer();
        List<String> created = createFixtureAccounts("next", 6, BigDecimal.valueOf(200));

        JsonNode firstPage = list(bearer, "?page=0&size=5&sortBy=iban&sortDirection=asc");
        JsonNode secondPage = list(bearer, "?page=1&size=5&sortBy=iban&sortDirection=asc");

        assertThat(secondPage.get("page").asInt()).isEqualTo(1);
        assertThat(secondPage.get("first").asBoolean()).isFalse();

        List<String> firstIbans = ibansOf(firstPage.get("content"));
        List<String> secondIbans = ibansOf(secondPage.get("content"));
        assertThat(firstIbans).doesNotContainAnyElementsOf(secondIbans);
        assertThat(firstIbans.size() + secondIbans.size()).isGreaterThanOrEqualTo(created.size());
    }

    @Test
    @DisplayName("The size parameter controls how many rows come back")
    void pageSize() throws Exception {
        String bearer = adminBearer();
        createFixtureAccounts("size", 3, BigDecimal.valueOf(300));

        JsonNode body = list(bearer, "?page=0&size=2");

        assertThat(body.get("size").asInt()).isEqualTo(2);
        assertThat(body.get("content")).hasSize(2);
    }

    @Test
    @DisplayName("sortDirection=asc orders the fixture rows by balance ascending")
    void sortAscending() throws Exception {
        String bearer = adminBearer();
        createFixtureAccounts("asc", 4, BigDecimal.valueOf(400));

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=balance&sortDirection=asc");
        List<Double> ordered = new ArrayList<>();
        body.get("content").forEach(node -> {
            double balance = node.get("balance").asDouble();
            if (balance >= 400.0 && balance < 500.0) {
                ordered.add(balance);
            }
        });

        assertThat(ordered).isSorted();
        assertThat(ordered).hasSize(4);
    }

    @Test
    @DisplayName("sortDirection=desc orders the fixture rows by balance descending")
    void sortDescending() throws Exception {
        String bearer = adminBearer();
        createFixtureAccounts("desc", 4, BigDecimal.valueOf(500));

        JsonNode body = list(bearer, "?page=0&size=100&sortBy=balance&sortDirection=desc");
        List<Double> ordered = new ArrayList<>();
        body.get("content").forEach(node -> {
            double balance = node.get("balance").asDouble();
            if (balance >= 500.0 && balance < 600.0) {
                ordered.add(balance);
            }
        });

        assertThat(ordered).isSortedAccordingTo((a, b) -> Double.compare(b, a));
        assertThat(ordered).hasSize(4);
    }

    @Test
    @DisplayName("An unknown sortBy field is rejected with 400")
    void invalidSortFieldIsRejected() throws Exception {
        String bearer = adminBearer();

        JsonNode problem = listExpectingBadRequest(bearer, "?sortBy=userId");

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

package bankingService.demo.support;

import bankingService.demo.domain.model.AccountStatus;
import bankingService.demo.domain.model.BankAccount;
import bankingService.demo.domain.port.out.AccountRepositoryPort;
import bankingService.demo.security.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared wiring for full-stack bankingService integration tests. The JWKS-backed decoder is swapped
 * for the in-memory {@link TestJwtDecoderConfig} one, and {@link UserServiceClient} - the outbound
 * userService hop - is mocked so account/card creation and password re-checks resolve without a live
 * userService. Each test runs in a rolled-back transaction on the {@code test} (H2) profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
public abstract class BankingIntegrationTest {

    protected static final UUID ADMIN_ID = UUID.fromString("ba3a83cd-a754-404b-a977-08f1c40dc4b6");
    protected static final UUID USER_ID = UUID.fromString("12d9ff81-1d75-4c56-acdd-3597207412bc");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestTokens tokens;

    @Autowired
    protected AccountRepositoryPort accountRepositoryPort;

    @MockitoBean
    protected UserServiceClient userServiceClient;

    @BeforeEach
    void stubUserService() {
        Mockito.when(userServiceClient.userExists(Mockito.any())).thenReturn(true);
        Mockito.when(userServiceClient.getCardholderName(Mockito.any())).thenReturn(Optional.of("Elena Dumitrescu"));
        Mockito.when(userServiceClient.verifyPassword(Mockito.any(), Mockito.any())).thenReturn(true);
    }

    protected String adminBearer() {
        return "Bearer " + tokens.validToken(ADMIN_ID, "cristina.ionescu", "ROLE_ADMIN");
    }

    protected String userBearer() {
        return "Bearer " + tokens.validToken(USER_ID, "elena.dumitrescu", "ROLE_USER");
    }

    protected String bearerFor(UUID userId) {
        return "Bearer " + tokens.validToken(userId, "some.user", "ROLE_USER");
    }

    /** Persists an ACTIVE account for {@code ownerId} directly, bypassing the API's userService check. */
    protected BankAccount saveAccount(UUID ownerId, BigDecimal balance) {
        String iban = ("TEST" + UUID.randomUUID()).replaceAll("[^A-Z0-9]", "").toUpperCase();
        iban = iban.substring(0, Math.min(iban.length(), 34));
        return accountRepositoryPort.save(BankAccount.builder()
                .userId(ownerId)
                .iban(iban)
                .currency("RON")
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .build());
    }

    protected JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
    }
}

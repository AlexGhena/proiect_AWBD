package userService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import userService.demo.domain.port.out.BankingServiceClientPort;
import userService.demo.support.TestRsaKeys;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin approve/reject workflow that turns a PENDING registration into a usable client account.
 * bankingService is never actually called here - {@link BankingServiceClientPort} is stubbed, the
 * same way bankingService's own tests never call the real userService.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationApprovalTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();
    private static final String ADMIN_NAME = "cristina.ionescu";
    private static final String ADMIN_PASSWORD = "AdminP@ss25";
    private static final String USER_NAME = "elena.dumitrescu";
    private static final String USER_PASSWORD = "Passw0rd!23";
    private static final String STUBBED_IBAN = "RO49AWBD0000000000000001";

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key", KEYS::privateKeyPem);
        registry.add("app.security.jwt.public-key", KEYS::publicKeyPem);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankingServiceClientPort bankingServiceClientPort;

    private String uniqueUsername() {
        return "appr." + UUID.randomUUID().toString().substring(0, 8);
    }

    private String bearerFor(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asString();
    }

    private String registerPendingUser(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"%s"}
                                """.formatted(username, username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    @DisplayName("Approving grants ROLE_USER, enables login, and returns the provisioned IBAN")
    void approvalEnablesLoginAndGrantsRoleUser() throws Exception {
        when(bankingServiceClientPort.provisionAccount(any())).thenReturn(STUBBED_IBAN);

        String username = uniqueUsername();
        String password = "RegPassw0rd!";
        String userId = registerPendingUser(username, password);
        String adminBearer = bearerFor(ADMIN_NAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/users/" + userId + "/approve").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.enabled").value(true))
                .andExpect(jsonPath("$.user.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.iban").value(STUBBED_IBAN));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.contains("ROLE_USER")));
    }

    @Test
    @DisplayName("Rejecting leaves the account disabled and roleless")
    void rejectionLeavesAccountDisabled() throws Exception {
        String username = uniqueUsername();
        String password = "RegPassw0rd!";
        String userId = registerPendingUser(username, password);
        String adminBearer = bearerFor(ADMIN_NAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/users/" + userId + "/reject").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(username, password)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A non-admin cannot approve or reject")
    void nonAdminCannotDecide() throws Exception {
        String userId = registerPendingUser(uniqueUsername(), "RegPassw0rd!");
        String userBearer = bearerFor(USER_NAME, USER_PASSWORD);

        mockMvc.perform(post("/api/users/" + userId + "/approve").header("Authorization", userBearer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/" + userId + "/reject").header("Authorization", userBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Approving an already-decided registration is rejected with 409")
    void approvingTwiceFails() throws Exception {
        when(bankingServiceClientPort.provisionAccount(any())).thenReturn(STUBBED_IBAN);

        String userId = registerPendingUser(uniqueUsername(), "RegPassw0rd!");
        String adminBearer = bearerFor(ADMIN_NAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/users/" + userId + "/approve").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/users/" + userId + "/approve").header("Authorization", adminBearer))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("The pending list is admin-only and reports only PENDING accounts")
    void pendingListIsAdminOnlyAndAccurate() throws Exception {
        String username = uniqueUsername();
        registerPendingUser(username, "RegPassw0rd!");
        String adminBearer = bearerFor(ADMIN_NAME, ADMIN_PASSWORD);
        String userBearer = bearerFor(USER_NAME, USER_PASSWORD);

        mockMvc.perform(get("/api/users/pending").header("Authorization", userBearer))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/pending").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username=='" + username + "')]").exists());
    }
}

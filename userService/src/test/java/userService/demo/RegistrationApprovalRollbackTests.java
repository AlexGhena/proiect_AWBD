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
import tools.jackson.databind.ObjectMapper;
import userService.demo.domain.exception.BankingProvisioningException;
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
 * Deliberately NOT class-level {@code @Transactional}: unlike {@link RegistrationApprovalTests}, this
 * verifies a real, committed database rollback across separate requests, which only happens if
 * approve()'s own {@code @Transactional} boundary is the outermost one for the request. Wrapping the
 * whole test method in one transaction (as the sibling class does for easy cleanup) would hide the
 * bug this test exists to catch, since nothing would ever really roll back mid-test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationApprovalRollbackTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();
    private static final String ADMIN_NAME = "cristina.ionescu";
    private static final String ADMIN_PASSWORD = "AdminP@ss25";

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

    @Test
    @DisplayName("If bankingService cannot provision an account, the approval rolls back entirely")
    void provisioningFailureRollsBackApproval() throws Exception {
        when(bankingServiceClientPort.provisionAccount(any()))
                .thenThrow(new BankingProvisioningException("bankingService unreachable", null));

        String username = "appr.rb." + UUID.randomUUID().toString().substring(0, 8);
        String password = "RegPassw0rd!";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"%s"}
                                """.formatted(username, username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("id").asString();

        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String adminBearer = "Bearer "
                + objectMapper.readTree(adminLogin.getResponse().getContentAsString()).get("accessToken").asString();

        mockMvc.perform(post("/api/users/" + userId + "/approve").header("Authorization", adminBearer))
                .andExpect(status().isBadGateway());

        // Neither the role grant nor the enable/approve flag survived the rollback.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","rememberMe":false}
                                """.formatted(username, password)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/pending").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username=='" + username + "')]").exists());
    }
}

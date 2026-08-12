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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import userService.demo.domain.port.out.UserRepositoryPort;
import userService.demo.support.TestRsaKeys;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Public self-registration must never be a route to elevated privileges. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key", KEYS::privateKeyPem);
        registry.add("app.security.jwt.public-key", KEYS::publicKeyPem);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    private String uniqueUsername() {
        return "reg." + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("A self-registered account is disabled, roleless and PENDING until an admin approves it")
    void registrationLeavesAccountPendingAndDisabled() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"RegPassw0rd!"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                // The password hash must never appear in a response.
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        // A PENDING account cannot log in at all - it has no role and login itself is disabled.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"RegPassw0rd!","rememberMe":false}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The stored credential is a BCrypt hash, never the raw password")
    void passwordIsStoredAsBcryptHash() throws Exception {
        String username = uniqueUsername();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"RegPassw0rd!"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated());

        String hash = userRepositoryPort.findByUsername(username).orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2");
        assertThat(hash).isNotEqualTo("RegPassw0rd!");
    }

    @Test
    @DisplayName("Creating a user through the admin endpoint is refused without authentication")
    void adminUserCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sneaky","email":"sneaky@example.com","password":"Passw0rd!23"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}

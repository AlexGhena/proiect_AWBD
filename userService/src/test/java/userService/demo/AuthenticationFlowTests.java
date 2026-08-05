package userService.demo;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import userService.demo.support.TestRsaKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the authentication surface: JDBC login, CSRF, remember-me, logout and the
 * shape of the issued token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();

    private static final String USER_NAME = "elena.dumitrescu";
    private static final String USER_PASSWORD = "Passw0rd!23";
    private static final String USER_ID = "12d9ff81-1d75-4c56-acdd-3597207412bc";
    private static final String ADMIN_NAME = "cristina.ionescu";
    private static final String ADMIN_PASSWORD = "AdminP@ss25";

    /** Signing keys are generated per run rather than reusing anything from the environment. */
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
    private JwtDecoder jwtDecoder;

    private String loginBody(String username, String password, boolean rememberMe) {
        return """
                {"username":"%s","password":"%s","rememberMe":%s}
                """.formatted(username, password, rememberMe);
    }

    private MvcResult login(String username, String password, boolean rememberMe) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password, rememberMe)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessTokenOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asString();
    }

    private String bearerFor(String username, String password) throws Exception {
        return "Bearer " + accessTokenOf(login(username, password, false));
    }

    /** Mirrors browser semantics when a response repeats a cookie: the last value wins. */
    private static Cookie lastCookieNamed(MvcResult result, String name) {
        Cookie[] cookies = result.getResponse().getCookies();
        Cookie found = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                found = cookie;
            }
        }
        return found;
    }

    @Test
    @DisplayName("JDBC login succeeds against the BCrypt hash in the project schema")
    void loginSucceedsWithBcryptPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USER_NAME, USER_PASSWORD, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.username").value(USER_NAME))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("A wrong password and an unknown username produce the identical 401")
    void badCredentialsAreIndistinguishable() throws Exception {
        String wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USER_NAME, "NotThePassword1", false)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownUser = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("no.such.person", "NotThePassword1", false)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Any difference here would let an attacker enumerate valid usernames.
        assertThat(wrongPassword).isEqualTo(unknownUser);
        assertThat(wrongPassword).doesNotContain("NotThePassword1");
    }

    @Test
    @DisplayName("A state-changing request without a CSRF token is rejected")
    void missingCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(USER_NAME, USER_PASSWORD, false)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/csrf hands the SPA a usable token")
    void csrfEndpointIssuesToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").value("_csrf"));
    }

    @Test
    @DisplayName("The issued JWT carries every required claim and verifies against the public key")
    void issuedTokenHasRequiredClaimsAndValidSignature() throws Exception {
        String token = accessTokenOf(login(USER_NAME, USER_PASSWORD, false));

        // Decoding also verifies the RS256 signature, the issuer and the audience.
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(USER_ID);
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo(USER_NAME);
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_USER");
        // Read as a raw string: the issuer is a plain identifier, and Jwt#getIssuer insists on a URL.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("userService");
        assertThat(jwt.getAudience()).containsExactly("awbd-banking-api");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull().isAfter(jwt.getIssuedAt());
        // Factor authorities such as FACTOR_PASSWORD must not leak into the roles claim.
        assertThat(jwt.getClaimAsStringList("roles")).allMatch(role -> role.startsWith("ROLE_"));
    }

    @Test
    @DisplayName("The remember-me cookie is set only when the client asks for it")
    void rememberMeCookieIsOptional() throws Exception {
        Cookie withoutRememberMe = login(USER_NAME, USER_PASSWORD, false)
                .getResponse().getCookie("remember-me");
        assertThat(withoutRememberMe).isNull();

        Cookie withRememberMe = login(USER_NAME, USER_PASSWORD, true)
                .getResponse().getCookie("remember-me");
        assertThat(withRememberMe).isNotNull();
        assertThat(withRememberMe.getValue()).isNotBlank();
        assertThat(withRememberMe.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("A remember-me cookie can mint a fresh access token after a browser reload")
    void rememberMeCookieIssuesNewAccessToken() throws Exception {
        Cookie rememberMe = login(USER_NAME, USER_PASSWORD, true)
                .getResponse().getCookie("remember-me");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/token")
                        .with(csrf())
                        .cookie(rememberMe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andReturn();

        assertThat(jwtDecoder.decode(accessTokenOf(refreshed)).getSubject()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Logout clears the remember-me cookie and the revoked cookie stops working")
    void logoutRevokesRememberMe() throws Exception {
        Cookie rememberMe = login(USER_NAME, USER_PASSWORD, true)
                .getResponse().getCookie("remember-me");

        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(rememberMe))
                .andExpect(status().isNoContent())
                .andReturn();

        // Consuming the cookie rotates the series before logout cancels it, so the response carries
        // two remember-me cookies. A browser applies them in order, so the last one is what sticks.
        Cookie cleared = lastCookieNamed(logout, "remember-me");
        assertThat(cleared).isNotNull();
        // Spring's cancelCookie writes a null value with a zero max-age.
        assertThat(cleared.getValue()).isNullOrEmpty();
        assertThat(cleared.getMaxAge()).isZero();

        // The series was deleted server side, so replaying the old cookie must not authenticate.
        mockMvc.perform(post("/api/auth/token")
                        .with(csrf())
                        .cookie(rememberMe))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoints reject requests with no token")
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A USER is refused the admin-only role endpoints, an ADMIN is allowed")
    void roleSeparationIsEnforced() throws Exception {
        mockMvc.perform(get("/api/roles").header("Authorization", bearerFor(USER_NAME, USER_PASSWORD)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/roles").header("Authorization", bearerFor(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A USER may read their own record but not another user's")
    void ownershipIsEnforcedOnUsers() throws Exception {
        String bearer = bearerFor(USER_NAME, USER_PASSWORD);

        mockMvc.perform(get("/api/users/" + USER_ID).header("Authorization", bearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/af944ee3-769a-4c79-b946-235768c1b3bf")
                        .header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("An invalid Bearer token is refused")
    void malformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}

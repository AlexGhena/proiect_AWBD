package userService.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import userService.demo.support.TestRsaKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CSRF contract the Angular client depends on, exercised without spring-security-test's
 * {@code csrf()} post-processor. That post-processor swaps in its own token repository, which would
 * hide the cookie name and header name the real configuration uses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfContractTests {

    private static final TestRsaKeys KEYS = TestRsaKeys.generate();

    @DynamicPropertySource
    static void jwtKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.private-key", KEYS::privateKeyPem);
        registry.add("app.security.jwt.public-key", KEYS::publicKeyPem);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("The CSRF endpoint publishes the XSRF cookie and header the SPA reads")
    void csrfEndpointPublishesSpaContract() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn();

        var cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        // Angular must be able to read it from document.cookie to echo it back.
        assertThat(cookie.isHttpOnly()).isFalse();
    }

    @Test
    @DisplayName("A token read from the cookie is accepted on the matching header")
    void tokenFromCookieIsAcceptedOnHeader() throws Exception {
        var primed = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var cookie = primed.getResponse().getCookie("XSRF-TOKEN");

        // Same request, once without the header and once with it.
        mockMvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"elena.dumitrescu","password":"Passw0rd!23","rememberMe":false}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                        .cookie(cookie)
                        .header("X-XSRF-TOKEN", cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"elena.dumitrescu","password":"Passw0rd!23","rememberMe":false}
                                """))
                .andExpect(status().isOk());
    }
}

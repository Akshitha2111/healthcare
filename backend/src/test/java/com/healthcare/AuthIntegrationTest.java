package com.healthcare;

import com.healthcare.support.IntegrationTestBase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {
    private static final String SECRET = "test-secret-test-secret-test-secret-1234567890";

    @Test
    void registerCreatesPatientSetsHttpOnlyCookieAndHidesPasswordHash() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register", null,
                        Map.of("fullName", "Asha Rao", "email", "Asha@Example.com", "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("asha@example.com"))
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password"))))
                .andReturn();
        Cookie cookie = r.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(profiles.count()).isEqualTo(1);
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        register("Asha", "asha@example.com");
        String hash = users.findByEmail("asha@example.com").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register("Asha", "asha@example.com");
        mvc.perform(post("/api/auth/register", null,
                        Map.of("fullName", "Other", "email", "ASHA@example.com", "password", PASSWORD)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register", null,
                        Map.of("fullName", "A", "email", "not-an-email", "password", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        mvc.perform(post("/api/auth/register", null,
                        Map.of("fullName", "A", "email", "a@example.com", "password", "onlyletters")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registrationCannotChooseRole() throws Exception {
        mvc.perform(post("/api/auth/register", null, Map.of("fullName", "Evil", "email", "evil@example.com",
                "password", PASSWORD, "role", "ADMIN"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void loginSucceedsWithCorrectCredentials() throws Exception {
        register("Asha", "asha@example.com");
        MvcResult r = mvc.perform(post("/api/auth/login", null, Map.of("email", "ASHA@example.com", "password", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.fullName").value("Asha")).andReturn();
        assertThat(r.getResponse().getCookie(COOKIE)).isNotNull();
    }

    @Test
    void invalidCredentialsGiveSameGenericMessage() throws Exception {
        register("Asha", "asha@example.com");
        mvc.perform(post("/api/auth/login", null, Map.of("email", "asha@example.com", "password", "Wrong12345")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login", null, Map.of("email", "nobody@example.com", "password", "Wrong12345")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void meReturnsCurrentUserFromCookieAndBearerToken() throws Exception {
        Cookie cookie = register("Asha", "asha@example.com");
        mvc.perform(get("/api/auth/me", cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@example.com"));
        mvc.perform(MockMvcRequestBuilders.get("/api/auth/me").header("Authorization", "Bearer " + cookie.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me", null)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tamperedExpiredAndForeignTokensAreRejected() throws Exception {
        Cookie good = register("Asha", "asha@example.com");
        Cookie tampered = new Cookie(COOKIE, good.getValue().substring(0, good.getValue().length() - 3) + "abc");
        mvc.perform(get("/api/auth/me", tampered)).andExpect(status().isUnauthorized());

        Long id = users.findByEmail("asha@example.com").orElseThrow().getId();
        String expired = Jwts.builder().subject(String.valueOf(id)).claim("role", "PATIENT")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200))).expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/auth/me", new Cookie(COOKIE, expired))).andExpect(status().isUnauthorized());

        String foreign = Jwts.builder().subject(String.valueOf(id)).claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("another-secret-another-secret-another-1234".getBytes(StandardCharsets.UTF_8)))
                .compact();
        mvc.perform(get("/api/auth/me", new Cookie(COOKIE, foreign))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me", new Cookie(COOKIE, "garbage"))).andExpect(status().isUnauthorized());
    }

    @Test
    void roleComesFromDatabaseNotFromTokenClaim() throws Exception {
        Cookie patient = register("Asha", "asha@example.com");
        Long id = users.findByEmail("asha@example.com").orElseThrow().getId();
        String forged = Jwts.builder().subject(String.valueOf(id)).claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/admin/dashboard", new Cookie(COOKIE, forged))).andExpect(status().isForbidden());
        assertThat(patient).isNotNull();
    }

    @Test
    void cookieAuthenticatedStateChangeWithoutCsrfHeaderIsRejected() throws Exception {
        Cookie cookie = register("Asha", "asha@example.com");
        mvc.perform(MockMvcRequestBuilders.post("/api/predictions").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"symptoms\":[\"itching\",\"skin_rash\"]}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_CHECK_FAILED"));
    }

    @Test
    void logoutClearsCookie() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/logout", null, null)).andExpect(status().isNoContent()).andReturn();
        assertThat(r.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0").contains(COOKIE);
    }
}

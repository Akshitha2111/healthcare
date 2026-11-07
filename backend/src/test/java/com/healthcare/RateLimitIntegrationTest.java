package com.healthcare;

import com.healthcare.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.rate-limit.auth-per-minute=3")
class RateLimitIntegrationTest extends IntegrationTestBase {

    @Test
    void repeatedLoginAttemptsAreThrottled() throws Exception {
        Map<String, String> body = Map.of("email", "nobody@example.com", "password", "Wrong12345");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login", null, body)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login", null, body)).andExpect(status().isTooManyRequests());
    }
}

package com.healthcare;

import com.healthcare.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAnalyticsIntegrationTest extends IntegrationTestBase {

    @Test
    void dashboardCountsRealData() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        register("Bala", "bala@example.com");
        mvc.perform(post("/api/predictions", a, Map.of("symptoms", List.of("itching", "skin_rash")))).andExpect(status().isCreated());
        saveDoctor("Dr. A", ALL_DAYS);
        Cookie admin = adminCookie();
        mvc.perform(get("/api/admin/dashboard", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(2))
                .andExpect(jsonPath("$.totalPredictions").value(1))
                .andExpect(jsonPath("$.activeDoctors").value(1))
                .andExpect(jsonPath("$.appointmentsByStatus.PENDING").value(0));
    }

    @Test
    void smallGroupsAreSuppressedInAnalytics() throws Exception {
        // min-group-size is 2 in the test profile: a symptom chosen by only one patient must not be reported
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        mvc.perform(post("/api/predictions", a, Map.of("symptoms", List.of("itching", "skin_rash")))).andExpect(status().isCreated());
        mvc.perform(post("/api/predictions", b, Map.of("symptoms", List.of("itching", "high_fever")))).andExpect(status().isCreated());
        Cookie admin = adminCookie();
        mvc.perform(get("/api/admin/analytics", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.minGroupSize").value(2))
                .andExpect(jsonPath("$.predictionsPerDay.length()").value(30))
                .andExpect(jsonPath("$.topSymptoms.length()").value(1))
                .andExpect(jsonPath("$.topSymptoms[0].label").value("Itching"))
                .andExpect(jsonPath("$.topSymptoms[0].count").value(2))
                .andExpect(jsonPath("$.modelUsage[0].count").value(2));
    }
}

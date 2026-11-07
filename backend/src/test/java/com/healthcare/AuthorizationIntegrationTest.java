package com.healthcare;

import com.healthcare.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationIntegrationTest extends IntegrationTestBase {

    @Test
    void anonymousUsersGet401OnProtectedRoutes() throws Exception {
        for (String url : new String[]{"/api/admin/dashboard", "/api/admin/analytics", "/api/admin/appointments",
                "/api/patients/me", "/api/predictions/my", "/api/appointments/my", "/api/doctors", "/api/symptoms"}) {
            mvc.perform(get(url, null)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void patientCannotReachAdminApis() throws Exception {
        Cookie patient = register("Asha", "asha@example.com");
        for (String url : new String[]{"/api/admin/dashboard", "/api/admin/analytics", "/api/admin/appointments",
                "/api/admin/doctors"}) {
            mvc.perform(get(url, patient)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        mvc.perform(patch("/api/admin/appointments/1/status", patient, java.util.Map.of("status", "CONFIRMED")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/doctors", patient, java.util.Map.of("name", "X"))).andExpect(status().isForbidden());
    }

    @Test
    void adminCanReachAdminApisButNotPatientOnlyApis() throws Exception {
        Cookie admin = adminCookie();
        mvc.perform(get("/api/admin/dashboard", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/analytics", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/appointments", admin)).andExpect(status().isOk());
        mvc.perform(get("/api/predictions/my", admin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/me", admin)).andExpect(status().isForbidden());
    }

    @Test
    void unknownRoutesDoNotLeakDetails() throws Exception {
        Cookie patient = register("Asha", "asha@example.com");
        mvc.perform(get("/api/does-not-exist", patient)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}

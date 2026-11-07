package com.healthcare;

import com.healthcare.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientProfileIntegrationTest extends IntegrationTestBase {

    @Test
    void patientUpdatesOnlyPermittedFields() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        Map<String, Object> req = new HashMap<>();
        req.put("fullName", "Asha R");
        req.put("phone", "+91 98765 43210");
        req.put("dateOfBirth", "1999-05-17");
        req.put("gender", "FEMALE");
        req.put("email", "hacker@example.com");   // ignored: not part of the request contract
        req.put("role", "ADMIN");
        mvc.perform(put("/api/patients/me", c, req)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha R")).andExpect(jsonPath("$.email").value("asha@example.com"));
        assertThat(users.findByEmail("asha@example.com").orElseThrow().getRole().name()).isEqualTo("PATIENT");
        mvc.perform(get("/api/patients/me", c)).andExpect(jsonPath("$.phone").value("+91 98765 43210"));
    }

    @Test
    void profileInputIsValidated() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        Map<String, Object> bad = new HashMap<>();
        bad.put("fullName", "");
        bad.put("phone", "abc");
        bad.put("dateOfBirth", "2999-01-01");
        bad.put("gender", "X");
        mvc.perform(put("/api/patients/me", c, bad)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists()).andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists()).andExpect(jsonPath("$.fieldErrors.gender").exists());
    }

    @Test
    void summaryUsesOnlyCallersData() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        mvc.perform(get("/api/patients/me/summary", a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPredictions").value(0)).andExpect(jsonPath("$.predictionsByMonth.length()").value(6));
    }
}

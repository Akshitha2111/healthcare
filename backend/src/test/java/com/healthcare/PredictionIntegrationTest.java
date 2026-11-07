package com.healthcare;

import com.healthcare.support.FakeAiServer;
import com.healthcare.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PredictionIntegrationTest extends IntegrationTestBase {

    private long create(Cookie c, String... symptoms) throws Exception {
        MvcResult r = mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of(symptoms))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void symptomCatalogIsProxiedFromAiService() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        mvc.perform(get("/api/symptoms", c)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.symptoms[0].name").value("itching")).andExpect(jsonPath("$.minSymptoms").value(2));
    }

    @Test
    void predictionIsReturnedAndPersisted() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("Itching", "skin rash"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.predictedClass").value("Fungal infection"))
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].rank").value(1))
                .andExpect(jsonPath("$.symptoms.length()").value(2))
                .andExpect(jsonPath("$.modelVersion").value("1.0.0-test"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty())
                .andExpect(jsonPath("$.urgentCareNotice").isNotEmpty())
                .andExpect(jsonPath("$.explanation.influentialSymptoms[0].influence").value(0.3));
        assertThat(predictions.count()).isEqualTo(1);
    }

    @Test
    void historyIsPaginatedFilteredAndOwnedByCaller() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        for (int i = 0; i < 3; i++) create(a, "itching", "skin_rash");
        create(b, "itching", "skin_rash");

        mvc.perform(get("/api/predictions/my?size=2", a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/predictions/my?q=fungal", b)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/predictions/my?q=nomatch", a)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/predictions/my?q=%25", a)).andExpect(status().isOk()); // wildcard is neutralised
    }

    @Test
    void patientCannotReadAnotherPatientsPrediction() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        long id = create(a, "itching", "skin_rash");
        mvc.perform(get("/api/predictions/" + id, a)).andExpect(status().isOk());
        mvc.perform(get("/api/predictions/" + id, b)).andExpect(status().isNotFound());
        mvc.perform(get("/api/predictions/999999", a)).andExpect(status().isNotFound());
    }

    @Test
    void inputIsValidated() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching", "<script>"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/predictions", c, Map.of())).andExpect(status().isBadRequest());
        // unsupported symptom: rejected by the AI service with 400 and relayed safely
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching", "bogus"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("unsupported symptom(s): bogus"));
        assertThat(predictions.count()).isZero();
    }

    @Test
    void aiTimeoutBecomes503AndNothingIsSaved() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        FakeAiServer.INSTANCE.delayMillis = 2500; // read timeout in the test profile is 800 ms
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching", "skin_rash"))))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
        assertThat(predictions.count()).isZero();
    }

    @Test
    void aiConnectionDropBecomes503() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        FakeAiServer.INSTANCE.mode = FakeAiServer.Mode.DROP_CONNECTION;
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching", "skin_rash"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void aiServerErrorBecomes502WithoutLeakingDetails() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        FakeAiServer.INSTANCE.mode = FakeAiServer.Mode.SERVER_ERROR;
        mvc.perform(post("/api/predictions", c, Map.of("symptoms", List.of("itching", "skin_rash"))))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("boom"))));
    }
}

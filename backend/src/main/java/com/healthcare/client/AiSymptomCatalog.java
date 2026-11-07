package com.healthcare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Mirrors the Flask GET /symptoms response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSymptomCatalog(
        int count,
        @JsonProperty("min_symptoms") int minSymptoms,
        @JsonProperty("max_symptoms") int maxSymptoms,
        List<AiSymptom> symptoms) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiSymptom(String name, String label) {}
}

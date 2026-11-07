package com.healthcare.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Mirrors the Flask POST /predict response (snake_case JSON). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiPredictResponse(
        boolean success,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("model_name") String modelName,
        @JsonProperty("score_type") String scoreType,
        @JsonProperty("top_prediction") String topPrediction,
        List<AiPrediction> predictions,
        @JsonProperty("symptoms_used") List<String> symptomsUsed,
        AiExplanation explanation,
        String description,
        @JsonProperty("general_guidance") List<String> generalGuidance,
        @JsonProperty("urgent_care_notice") String urgentCareNotice,
        List<String> limitations,
        String disclaimer) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiPrediction(int rank, String disease, double score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiExplanation(String method,
                                @JsonProperty("influential_symptoms") List<AiInfluence> influentialSymptoms,
                                String caveat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiInfluence(String symptom, String label, double influence) {}
}

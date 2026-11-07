package com.healthcare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class PredictionDtos {
    private PredictionDtos() {}

    public record CreatePredictionRequest(
            @NotNull @Size(min = 2, max = 20, message = "Select between 2 and 20 symptoms")
            List<@Size(max = 60) @Pattern(regexp = "^[A-Za-z0-9 _-]+$", message = "Invalid symptom name") String>
                    symptoms) {}

    public record SymptomItem(String name, String label) {}

    public record ResultItem(int rank, String disease, double score) {}

    public record Influence(String symptom, String label, double influence) {}

    public record ExplanationDto(String method, List<Influence> influentialSymptoms, String caveat) {}

    public record PredictionDto(Long id, Instant createdAt, String predictedClass, String modelName,
                                String modelVersion, String scoreType, List<SymptomItem> symptoms,
                                List<ResultItem> results, String description, List<String> generalGuidance,
                                String urgentCareNotice, List<String> limitations, ExplanationDto explanation,
                                String disclaimer) {}

    public record PredictionSummaryDto(Long id, Instant createdAt, String predictedClass, Double topScore,
                                       List<String> symptoms, String modelVersion) {}

    public record SymptomCatalogDto(int count, int minSymptoms, int maxSymptoms, List<SymptomItem> symptoms) {}
}

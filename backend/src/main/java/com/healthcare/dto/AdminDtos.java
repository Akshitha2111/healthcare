package com.healthcare.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AdminDtos {
    private AdminDtos() {}

    public record DashboardDto(long totalPatients, long newPatientsLast30Days, long activeDoctors,
                               long totalAppointments, long pendingAppointments,
                               Map<String, Long> appointmentsByStatus, long totalPredictions,
                               long predictionsLast7Days) {}

    public record LabelCount(String label, long count) {}

    public record DayCount(LocalDate date, long count) {}

    public record ModelUsage(String modelName, String modelVersion, long count) {}

    /** Aggregates only. Groups with fewer than minGroupSize distinct patients are omitted (privacy safeguard). */
    public record AnalyticsDto(int minGroupSize, List<DayCount> predictionsPerDay, List<LabelCount> topSymptoms,
                               List<LabelCount> topPredictedConditions, List<ModelUsage> modelUsage) {}
}

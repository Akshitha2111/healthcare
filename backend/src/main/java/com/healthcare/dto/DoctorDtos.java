package com.healthcare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public final class DoctorDtos {
    private DoctorDtos() {}

    public record DoctorDto(Long id, String name, String specialty, List<String> availableDays, LocalTime startTime,
                            LocalTime endTime, int slotMinutes, boolean active) {}

    /** availableDays: java.time.DayOfWeek names, e.g. ["MONDAY","WEDNESDAY"]. */
    public record DoctorRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String specialty,
            @NotEmpty List<String> availableDays,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(10) @Max(120) int slotMinutes,
            Boolean active) {}
}

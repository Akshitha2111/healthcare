package com.healthcare.dto;

import com.healthcare.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class AppointmentDtos {
    private AppointmentDtos() {}

    public record BookRequest(@NotNull Long doctorId, @NotNull LocalDate date, @NotNull LocalTime time) {}

    public record SlotDto(LocalTime time, boolean available) {}

    public record AppointmentDto(Long id, Long doctorId, String doctorName, String specialty, LocalDate date,
                                 LocalTime time, AppointmentStatus status, boolean cancellable, Instant createdAt) {}

    public record UpdateStatusRequest(@NotNull AppointmentStatus status) {}

    public record AdminAppointmentDto(Long id, String patientName, Long doctorId, String doctorName,
                                      String specialty, LocalDate date, LocalTime time, AppointmentStatus status,
                                      List<AppointmentStatus> allowedTransitions, Instant createdAt) {}
}

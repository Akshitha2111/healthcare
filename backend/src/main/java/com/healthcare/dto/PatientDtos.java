package com.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PatientDtos {
    private PatientDtos() {}

    public record ProfileDto(String fullName, String email, String phone, LocalDate dateOfBirth, String gender,
                             Instant memberSince) {}

    /** Only these fields can be changed by the patient. Email and role cannot. */
    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^[0-9+()\\- ]{7,20}$", message = "Enter a valid phone number") String phone,
            @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
            @Pattern(regexp = "^(FEMALE|MALE|OTHER|PREFER_NOT_TO_SAY)$", message = "Invalid gender value")
            String gender) {}

    public record MonthCount(String month, long count) {}

    public record SummaryDto(long totalPredictions, long upcomingAppointments, List<MonthCount> predictionsByMonth) {}
}

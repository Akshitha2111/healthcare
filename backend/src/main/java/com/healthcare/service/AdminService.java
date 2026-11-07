package com.healthcare.service;

import com.healthcare.config.AppProperties;
import com.healthcare.dto.AdminDtos.AnalyticsDto;
import com.healthcare.dto.AdminDtos.DashboardDto;
import com.healthcare.dto.AdminDtos.DayCount;
import com.healthcare.dto.AdminDtos.LabelCount;
import com.healthcare.dto.AdminDtos.ModelUsage;
import com.healthcare.entity.AppointmentStatus;
import com.healthcare.entity.Role;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PredictionRepository;
import com.healthcare.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregate-only reporting. No endpoint here returns an individual patient's predictions or symptoms. */
@Service
@Transactional(readOnly = true)
public class AdminService {
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final PredictionRepository predictions;
    private final int minGroupSize;
    private final ZoneId zone;

    public AdminService(UserRepository users, DoctorRepository doctors, AppointmentRepository appointments,
                        PredictionRepository predictions, AppProperties props) {
        this.users = users;
        this.doctors = doctors;
        this.appointments = appointments;
        this.predictions = predictions;
        this.minGroupSize = props.analytics().minGroupSize();
        this.zone = ZoneId.of(props.clinicTimezone());
    }

    public DashboardDto dashboard() {
        Map<AppointmentStatus, Long> byStatus = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus s : AppointmentStatus.values()) {
            byStatus.put(s, 0L);
        }
        for (Object[] row : appointments.countByStatus()) {
            byStatus.put((AppointmentStatus) row[0], ((Number) row[1]).longValue());
        }
        Map<String, Long> statusMap = new LinkedHashMap<>();
        byStatus.forEach((k, v) -> statusMap.put(k.name(), v));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        Instant now = Instant.now();
        return new DashboardDto(
                users.countByRole(Role.PATIENT),
                users.countByRoleAndCreatedAtAfter(Role.PATIENT, now.minus(30, ChronoUnit.DAYS)),
                doctors.countByActiveTrue(),
                total,
                byStatus.get(AppointmentStatus.PENDING),
                statusMap,
                predictions.count(),
                predictions.countByCreatedAtAfter(now.minus(7, ChronoUnit.DAYS)));
    }

    public AnalyticsDto analytics() {
        LocalDate today = LocalDate.now(zone);
        LocalDate first = today.minusDays(29);
        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            perDay.put(d, 0L);
        }
        for (Instant created : predictions.createdAtSince(first.atStartOfDay(zone).toInstant())) {
            perDay.computeIfPresent(created.atZone(zone).toLocalDate(), (k, v) -> v + 1);
        }
        List<DayCount> days = new ArrayList<>();
        perDay.forEach((d, c) -> days.add(new DayCount(d, c)));

        List<LabelCount> topSymptoms = predictions.topSymptoms(minGroupSize, PageRequest.of(0, 10)).stream()
                .map(r -> new LabelCount(Labels.symptom((String) r[0]), ((Number) r[1]).longValue())).toList();
        List<LabelCount> topConditions = predictions.topPredictedClasses(minGroupSize, PageRequest.of(0, 10)).stream()
                .map(r -> new LabelCount((String) r[0], ((Number) r[1]).longValue())).toList();
        List<ModelUsage> usage = predictions.modelUsage().stream()
                .map(r -> new ModelUsage((String) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
        return new AnalyticsDto(minGroupSize, days, topSymptoms, topConditions, usage);
    }
}

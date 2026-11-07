package com.healthcare.service;

import com.healthcare.config.AppProperties;
import com.healthcare.dto.PatientDtos.MonthCount;
import com.healthcare.dto.PatientDtos.ProfileDto;
import com.healthcare.dto.PatientDtos.SummaryDto;
import com.healthcare.dto.PatientDtos.UpdateProfileRequest;
import com.healthcare.entity.PatientProfile;
import com.healthcare.entity.User;
import com.healthcare.exception.ApiException;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.PatientProfileRepository;
import com.healthcare.repository.PredictionRepository;
import com.healthcare.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PatientService {
    private final UserRepository users;
    private final PatientProfileRepository profiles;
    private final PredictionRepository predictions;
    private final AppointmentRepository appointments;
    private final ZoneId zone;

    public PatientService(UserRepository users, PatientProfileRepository profiles, PredictionRepository predictions,
                          AppointmentRepository appointments, AppProperties props) {
        this.users = users;
        this.profiles = profiles;
        this.predictions = predictions;
        this.appointments = appointments;
        this.zone = ZoneId.of(props.clinicTimezone());
    }

    public ProfileDto profile(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
        PatientProfile p = profiles.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
        return toDto(user, p);
    }

    @Transactional
    public ProfileDto update(Long userId, UpdateProfileRequest req) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
        PatientProfile p = profiles.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
        user.setFullName(req.fullName().trim());
        p.setPhone(blankToNull(req.phone()));
        p.setDateOfBirth(req.dateOfBirth());
        p.setGender(blankToNull(req.gender()));
        return toDto(user, p);
    }

    /** Dashboard numbers computed from the caller's own data only. */
    public SummaryDto summary(Long userId) {
        long total = predictions.countByPatientId(userId);
        LocalDateTime now = LocalDateTime.now(zone);
        long upcoming = appointments.upcoming(userId, now.toLocalDate(), now.toLocalTime(), PageRequest.of(0, 1))
                .getTotalElements();

        YearMonth current = YearMonth.now(zone);
        Instant since = current.minusMonths(5).atDay(1).atStartOfDay(zone).toInstant();
        Map<YearMonth, Long> buckets = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            buckets.put(current.minusMonths(i), 0L);
        }
        for (Instant created : predictions.createdAtSince(userId, since)) {
            buckets.merge(YearMonth.from(created.atZone(zone)), 1L, Long::sum);
        }
        List<MonthCount> months = new ArrayList<>();
        buckets.forEach((m, c) -> months.add(new MonthCount(m.toString(), c)));
        return new SummaryDto(total, upcoming, months);
    }

    private static ProfileDto toDto(User u, PatientProfile p) {
        return new ProfileDto(u.getFullName(), u.getEmail(), p.getPhone(), p.getDateOfBirth(), p.getGender(),
                u.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

package com.healthcare.service;

import com.healthcare.dto.DoctorDtos.DoctorDto;
import com.healthcare.dto.DoctorDtos.DoctorRequest;
import com.healthcare.entity.Doctor;
import com.healthcare.exception.ApiException;
import com.healthcare.repository.DoctorRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DoctorService {
    private final DoctorRepository doctors;

    public DoctorService(DoctorRepository doctors) {
        this.doctors = doctors;
    }

    public List<DoctorDto> listActive() {
        return doctors.findByActiveTrueOrderByNameAsc().stream().map(DoctorService::toDto).toList();
    }

    public List<DoctorDto> listAll() {
        return doctors.findAllByOrderByNameAsc().stream().map(DoctorService::toDto).toList();
    }

    @Transactional
    public DoctorDto create(DoctorRequest req) {
        Doctor d = new Doctor();
        apply(d, req);
        return toDto(doctors.save(d));
    }

    /** Changing availability never cancels existing appointments; it only affects new bookings. */
    @Transactional
    public DoctorDto update(Long id, DoctorRequest req) {
        Doctor d = doctors.findById(id).orElseThrow(() -> ApiException.notFound("Doctor not found"));
        apply(d, req);
        return toDto(d);
    }

    /** All slot start times for a doctor (shared by validation and slot listing). */
    public static List<LocalTime> slotTimes(Doctor d) {
        List<LocalTime> out = new ArrayList<>();
        LocalTime t = d.getStartTime();
        while (true) {
            LocalTime end = t.plusMinutes(d.getSlotMinutes());
            if (end.isBefore(t) || end.isAfter(d.getEndTime())) { // wrapped past midnight, or beyond closing time
                break;
            }
            out.add(t);
            t = end;
            if (t.equals(d.getEndTime())) {
                break;
            }
        }
        return out;
    }

    private void apply(Doctor d, DoctorRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw ApiException.badRequest("Start time must be before end time");
        }
        List<DayOfWeek> days;
        try {
            days = req.availableDays().stream().map(s -> DayOfWeek.valueOf(s.trim().toUpperCase(Locale.ROOT)))
                    .distinct().sorted().toList();
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("availableDays must be day names such as MONDAY");
        }
        d.setName(req.name().trim());
        d.setSpecialty(req.specialty().trim());
        d.setAvailableDays(days.stream().map(Enum::name).collect(Collectors.joining(",")));
        d.setStartTime(req.startTime().withSecond(0).withNano(0));
        d.setEndTime(req.endTime().withSecond(0).withNano(0));
        d.setSlotMinutes(req.slotMinutes());
        d.setActive(req.active() == null || req.active());
        if (slotTimes(d).isEmpty()) {
            throw ApiException.badRequest("The time window is shorter than one appointment slot");
        }
    }

    public static DoctorDto toDto(Doctor d) {
        return new DoctorDto(d.getId(), d.getName(), d.getSpecialty(),
                Arrays.stream(d.getAvailableDays().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),
                d.getStartTime(), d.getEndTime(), d.getSlotMinutes(), d.isActive());
    }
}

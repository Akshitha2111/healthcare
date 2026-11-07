package com.healthcare.service;

import com.healthcare.config.AppProperties;
import com.healthcare.dto.AppointmentDtos.AdminAppointmentDto;
import com.healthcare.dto.AppointmentDtos.AppointmentDto;
import com.healthcare.dto.AppointmentDtos.BookRequest;
import com.healthcare.dto.AppointmentDtos.SlotDto;
import com.healthcare.dto.CommonDtos.PageResponse;
import com.healthcare.entity.Appointment;
import com.healthcare.entity.AppointmentStatus;
import com.healthcare.entity.Doctor;
import com.healthcare.exception.ApiException;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AppointmentService {
    static final int MAX_DAYS_AHEAD = 90;

    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final UserRepository users;
    private final ZoneId zone;

    public AppointmentService(AppointmentRepository appointments, DoctorRepository doctors, UserRepository users,
                              AppProperties props) {
        this.appointments = appointments;
        this.doctors = doctors;
        this.users = users;
        this.zone = ZoneId.of(props.clinicTimezone());
    }

    // ---- slots -----------------------------------------------------------
    public List<SlotDto> slots(Long doctorId, LocalDate date) {
        Doctor d = doctors.findById(doctorId).filter(Doctor::isActive)
                .orElseThrow(() -> ApiException.notFound("Doctor not found"));
        if (!d.days().contains(date.getDayOfWeek())) {
            return List.of();
        }
        Set<LocalTime> booked = new HashSet<>(appointments.bookedTimes(doctorId, date));
        LocalDateTime now = LocalDateTime.now(zone);
        return DoctorService.slotTimes(d).stream()
                .map(t -> new SlotDto(t, !booked.contains(t) && LocalDateTime.of(date, t).isAfter(now))).toList();
    }

    // ---- patient operations ------------------------------------------------
    @Transactional
    public AppointmentDto book(Long patientId, BookRequest req) {
        Doctor d = doctors.findById(req.doctorId()).filter(Doctor::isActive)
                .orElseThrow(() -> ApiException.notFound("Doctor not found"));
        LocalDate date = req.date();
        LocalTime time = req.time().withSecond(0).withNano(0);
        LocalDateTime now = LocalDateTime.now(zone);

        if (!LocalDateTime.of(date, time).isAfter(now)) {
            throw ApiException.badRequest("The appointment must be in the future");
        }
        if (date.isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            throw ApiException.badRequest("Appointments can be booked up to " + MAX_DAYS_AHEAD + " days ahead");
        }
        if (!d.days().contains(date.getDayOfWeek())) {
            throw ApiException.badRequest("The doctor is not available on this day");
        }
        if (!DoctorService.slotTimes(d).contains(time)) {
            throw ApiException.badRequest("That is not a valid time slot for this doctor");
        }
        if (appointments.doctorSlotTaken(d.getId(), date, time)) {
            throw ApiException.conflict("This time slot is no longer available");
        }
        if (appointments.patientSlotTaken(patientId, date, time)) {
            throw ApiException.conflict("You already have an appointment at this time");
        }

        Appointment a = new Appointment();
        a.setPatient(users.getReferenceById(patientId));
        a.setDoctor(d);
        a.setAppointmentDate(date);
        a.setAppointmentTime(time);
        a.changeStatus(AppointmentStatus.PENDING);
        try {
            appointments.saveAndFlush(a); // unique constraints catch races between concurrent bookings
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("This time slot is no longer available");
        }
        return toDto(a);
    }

    public PageResponse<AppointmentDto> mine(Long patientId, AppointmentStatus status, boolean upcomingOnly,
                                             Pageable pageable) {
        LocalDateTime now = LocalDateTime.now(zone);
        Page<Appointment> page;
        if (upcomingOnly) {
            page = appointments.upcoming(patientId, now.toLocalDate(), now.toLocalTime(), pageable);
        } else if (status != null) {
            page = appointments.findByPatientIdAndStatus(patientId, status, pageable);
        } else {
            page = appointments.findByPatientId(patientId, pageable);
        }
        return PageResponse.of(page.map(this::toDto));
    }

    @Transactional
    public AppointmentDto cancel(Long patientId, Long appointmentId) {
        Appointment a = appointments.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!a.getStatus().occupiesSlot()) {
            throw ApiException.conflict("Only pending or confirmed appointments can be cancelled");
        }
        if (!isInFuture(a)) {
            throw ApiException.conflict("Past appointments cannot be cancelled");
        }
        a.changeStatus(AppointmentStatus.CANCELLED);
        return toDto(a);
    }

    // ---- admin operations --------------------------------------------------
    public PageResponse<AdminAppointmentDto> adminList(AppointmentStatus status, Pageable pageable) {
        Page<Appointment> page = status == null ? appointments.findAll(pageable)
                : appointments.findByStatus(status, pageable);
        return PageResponse.of(page.map(this::toAdminDto));
    }

    @Transactional
    public AdminAppointmentDto updateStatus(Long appointmentId, AppointmentStatus next) {
        Appointment a = appointments.findWithDetailsById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));
        if (!a.getStatus().allowedNext().contains(next)) {
            throw ApiException.conflict("Cannot change status from " + a.getStatus() + " to " + next);
        }
        if (next == AppointmentStatus.COMPLETED && isInFuture(a)) {
            throw ApiException.conflict("A future appointment cannot be marked as completed");
        }
        a.changeStatus(next);
        return toAdminDto(a);
    }

    // ---- mapping -----------------------------------------------------------
    private boolean isInFuture(Appointment a) {
        return LocalDateTime.of(a.getAppointmentDate(), a.getAppointmentTime()).isAfter(LocalDateTime.now(zone));
    }

    private AppointmentDto toDto(Appointment a) {
        Doctor d = a.getDoctor();
        return new AppointmentDto(a.getId(), d.getId(), d.getName(), d.getSpecialty(), a.getAppointmentDate(),
                a.getAppointmentTime(), a.getStatus(), a.getStatus().occupiesSlot() && isInFuture(a),
                a.getCreatedAt());
    }

    private AdminAppointmentDto toAdminDto(Appointment a) {
        Doctor d = a.getDoctor();
        return new AdminAppointmentDto(a.getId(), a.getPatient().getFullName(), d.getId(), d.getName(),
                d.getSpecialty(), a.getAppointmentDate(), a.getAppointmentTime(), a.getStatus(),
                a.getStatus().allowedNext().stream().sorted().toList(), a.getCreatedAt());
    }
}

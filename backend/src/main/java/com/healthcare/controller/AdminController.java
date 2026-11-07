package com.healthcare.controller;

import com.healthcare.dto.AdminDtos.AnalyticsDto;
import com.healthcare.dto.AdminDtos.DashboardDto;
import com.healthcare.dto.AppointmentDtos.AdminAppointmentDto;
import com.healthcare.dto.AppointmentDtos.UpdateStatusRequest;
import com.healthcare.dto.CommonDtos.PageResponse;
import com.healthcare.dto.DoctorDtos.DoctorDto;
import com.healthcare.dto.DoctorDtos.DoctorRequest;
import com.healthcare.entity.AppointmentStatus;
import com.healthcare.service.AdminService;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import com.healthcare.service.PageUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Every route under /api/admin/** requires ROLE_ADMIN (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final AppointmentService appointments;
    private final DoctorService doctors;

    public AdminController(AdminService admin, AppointmentService appointments, DoctorService doctors) {
        this.admin = admin;
        this.appointments = appointments;
        this.doctors = doctors;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return admin.dashboard();
    }

    @GetMapping("/analytics")
    public AnalyticsDto analytics() {
        return admin.analytics();
    }

    @GetMapping("/appointments")
    public PageResponse<AdminAppointmentDto> appointments(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) AppointmentStatus status) {
        Sort sort = Sort.by("appointmentDate").descending().and(Sort.by("appointmentTime").descending());
        return appointments.adminList(status, PageUtil.of(page, size, sort));
    }

    @PatchMapping("/appointments/{id}/status")
    public AdminAppointmentDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest req) {
        return appointments.updateStatus(id, req.status());
    }

    @GetMapping("/doctors")
    public List<DoctorDto> doctors() {
        return doctors.listAll();
    }

    @PostMapping("/doctors")
    public ResponseEntity<DoctorDto> createDoctor(@Valid @RequestBody DoctorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctors.create(req));
    }

    @PutMapping("/doctors/{id}")
    public DoctorDto updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest req) {
        return doctors.update(id, req);
    }
}

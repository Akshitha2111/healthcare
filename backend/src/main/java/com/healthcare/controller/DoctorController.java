package com.healthcare.controller;

import com.healthcare.dto.AppointmentDtos.SlotDto;
import com.healthcare.dto.DoctorDtos.DoctorDto;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.DoctorService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService doctors;
    private final AppointmentService appointments;

    public DoctorController(DoctorService doctors, AppointmentService appointments) {
        this.doctors = doctors;
        this.appointments = appointments;
    }

    @GetMapping
    public List<DoctorDto> list() {
        return doctors.listActive();
    }

    @GetMapping("/{id}/slots")
    public List<SlotDto> slots(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointments.slots(id, date);
    }
}

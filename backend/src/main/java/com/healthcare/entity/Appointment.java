package com.healthcare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * slotActive is TRUE while the appointment holds its slot and NULL afterwards. Unique constraints over
 * (doctor, date, time, slotActive) and (patient, date, time, slotActive) therefore make double booking
 * impossible at database level even under concurrent requests, while cancelled slots can be re-booked.
 */
@Entity
@Table(name = "appointments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_appt_doctor_slot",
                columnNames = {"doctor_id", "appointment_date", "appointment_time", "slot_active"}),
        @UniqueConstraint(name = "uq_appt_patient_slot",
                columnNames = {"patient_id", "appointment_date", "appointment_time", "slot_active"})})
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "slot_active")
    private Boolean slotActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    /** Sets the status and keeps slotActive consistent with it. */
    public void changeStatus(AppointmentStatus newStatus) {
        this.status = newStatus;
        this.slotActive = newStatus.occupiesSlot() ? Boolean.TRUE : null;
    }

    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public Boolean getSlotActive() { return slotActive; }
    public Instant getCreatedAt() { return createdAt; }
}

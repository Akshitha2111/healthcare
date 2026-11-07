package com.healthcare.repository;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"doctor"})
    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    @EntityGraph(attributePaths = {"doctor"})
    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"doctor"})
    @Query("select a from Appointment a where a.patient.id = :uid and a.slotActive = true "
            + "and (a.appointmentDate > :today or (a.appointmentDate = :today and a.appointmentTime >= :now))")
    Page<Appointment> upcoming(@Param("uid") Long patientId, @Param("today") LocalDate today,
                               @Param("now") LocalTime now, Pageable pageable);

    /** Ownership is part of the query. */
    @EntityGraph(attributePaths = {"doctor"})
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    @Query("select a.appointmentTime from Appointment a where a.doctor.id = :did "
            + "and a.appointmentDate = :date and a.slotActive = true")
    List<LocalTime> bookedTimes(@Param("did") Long doctorId, @Param("date") LocalDate date);

    @Query("select count(a) > 0 from Appointment a where a.doctor.id = :did and a.appointmentDate = :date "
            + "and a.appointmentTime = :time and a.slotActive = true")
    boolean doctorSlotTaken(@Param("did") Long doctorId, @Param("date") LocalDate date,
                            @Param("time") LocalTime time);

    @Query("select count(a) > 0 from Appointment a where a.patient.id = :uid and a.appointmentDate = :date "
            + "and a.appointmentTime = :time and a.slotActive = true")
    boolean patientSlotTaken(@Param("uid") Long patientId, @Param("date") LocalDate date,
                             @Param("time") LocalTime time);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Appointment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Optional<Appointment> findWithDetailsById(Long id);

    @Query("select a.status, count(a) from Appointment a group by a.status")
    List<Object[]> countByStatus();
}

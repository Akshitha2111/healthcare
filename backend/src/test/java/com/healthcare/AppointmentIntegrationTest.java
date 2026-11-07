package com.healthcare;

import com.healthcare.entity.Appointment;
import com.healthcare.entity.AppointmentStatus;
import com.healthcare.entity.Doctor;
import com.healthcare.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentIntegrationTest extends IntegrationTestBase {
    private final String date = LocalDate.now().plusDays(3).toString();

    private Map<String, Object> body(Doctor d, String date, String time) {
        return Map.of("doctorId", d.getId(), "date", date, "time", time);
    }

    private long book(Cookie c, Doctor d, String time) throws Exception {
        MvcResult r = mvc.perform(post("/api/appointments", c, body(d, date, time))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void doctorsAndSlotsAreListed() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        Doctor inactive = saveDoctor("Dr. Hidden", ALL_DAYS);
        inactive.setActive(false);
        doctors.save(inactive);
        mvc.perform(get("/api/doctors", c)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/doctors/" + d.getId() + "/slots?date=" + date, c)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16)).andExpect(jsonPath("$[0].available").value(true));
        book(c, d, "09:00");
        mvc.perform(get("/api/doctors/" + d.getId() + "/slots?date=" + date, c)).andExpect(jsonPath("$[0].available").value(false));
    }

    @Test
    void bookingSucceedsAndAppearsInMyAppointments() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        book(c, d, "10:00");
        mvc.perform(get("/api/appointments/my", c)).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr. A"))
                .andExpect(jsonPath("$.content[0].cancellable").value(true));
        mvc.perform(get("/api/appointments/my?upcoming=true", c)).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void doubleBookingSameSlotIsRejected() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        book(a, d, "10:00");
        mvc.perform(post("/api/appointments", b, body(d, date, "10:00"))).andExpect(status().isConflict());
    }

    @Test
    void patientCannotHoldTwoAppointmentsAtTheSameTime() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Doctor d1 = saveDoctor("Dr. A", ALL_DAYS);
        Doctor d2 = saveDoctor("Dr. B", ALL_DAYS);
        book(a, d1, "10:00");
        mvc.perform(post("/api/appointments", a, body(d2, date, "10:00"))).andExpect(status().isConflict());
    }

    @Test
    void invalidBookingsAreRejected() throws Exception {
        Cookie c = register("Asha", "asha@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        String yesterday = LocalDate.now().minusDays(1).toString();
        mvc.perform(post("/api/appointments", c, body(d, yesterday, "10:00"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/appointments", c, body(d, date, "10:10"))).andExpect(status().isBadRequest());   // not on a slot boundary
        mvc.perform(post("/api/appointments", c, body(d, date, "20:00"))).andExpect(status().isBadRequest());   // after hours
        mvc.perform(post("/api/appointments", c, body(d, LocalDate.now().plusDays(200).toString(), "10:00")))
                .andExpect(status().isBadRequest());                                                             // too far ahead
        mvc.perform(post("/api/appointments", c, Map.of("doctorId", 99999, "date", date, "time", "10:00")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/appointments", c, Map.of("date", date))).andExpect(status().isBadRequest());

        String otherDay = LocalDate.parse(date).getDayOfWeek().name();
        Doctor offDay = saveDoctor("Dr. Off", ALL_DAYS.replace(otherDay, "").replace(",,", ","));
        mvc.perform(post("/api/appointments", c, body(offDay, date, "10:00"))).andExpect(status().isBadRequest());
    }

    @Test
    void cancellingFreesTheSlotAndOnlyOwnerCanCancel() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        long id = book(a, d, "10:00");

        mvc.perform(patch("/api/appointments/" + id + "/cancel", b, null)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/appointments/" + id + "/cancel", a, null)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(patch("/api/appointments/" + id + "/cancel", a, null)).andExpect(status().isConflict());
        mvc.perform(post("/api/appointments", b, body(d, date, "10:00"))).andExpect(status().isCreated());
    }

    @Test
    void patientsOnlySeeTheirOwnAppointments() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie b = register("Bala", "bala@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        book(a, d, "10:00");
        mvc.perform(get("/api/appointments/my", b)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pastAppointmentsCannotBeCancelled() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        Appointment past = new Appointment();
        past.setPatient(users.findByEmail("asha@example.com").orElseThrow());
        past.setDoctor(d);
        past.setAppointmentDate(LocalDate.now().minusDays(2));
        past.setAppointmentTime(LocalTime.of(10, 0));
        past.changeStatus(AppointmentStatus.CONFIRMED);
        past = appointments.save(past);
        mvc.perform(patch("/api/appointments/" + past.getId() + "/cancel", a, null)).andExpect(status().isConflict());
    }

    @Test
    void adminManagesStatusWithValidTransitionsOnly() throws Exception {
        Cookie a = register("Asha", "asha@example.com");
        Cookie admin = adminCookie();
        Doctor d = saveDoctor("Dr. A", ALL_DAYS);
        long id = book(a, d, "10:00");

        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "COMPLETED")))
                .andExpect(status().isConflict());                    // PENDING -> COMPLETED not allowed
        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "CONFIRMED")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.patientName").value("Asha"));
        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "COMPLETED")))
                .andExpect(status().isConflict());                    // future appointment cannot be completed
        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "CANCELLED")))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "CONFIRMED")))
                .andExpect(status().isConflict());                    // CANCELLED is final
        mvc.perform(patch("/api/admin/appointments/" + id + "/status", admin, Map.of("status", "NOPE")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/appointments?status=CANCELLED", admin)).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void adminCanManageDoctorAvailability() throws Exception {
        Cookie admin = adminCookie();
        Map<String, Object> req = Map.of("name", "Dr. New", "specialty", "Cardiology",
                "availableDays", java.util.List.of("monday", "WEDNESDAY"), "startTime", "09:00", "endTime", "12:00",
                "slotMinutes", 30);
        MvcResult r = mvc.perform(post("/api/admin/doctors", admin, req)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableDays[0]").value("MONDAY")).andReturn();
        long id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/admin/doctors/" + id, admin, Map.of("name", "Dr. New", "specialty", "Cardiology",
                "availableDays", java.util.List.of("FRIDAY"), "startTime", "13:00", "endTime", "15:00",
                "slotMinutes", 20, "active", false))).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(put("/api/admin/doctors/" + id, admin, Map.of("name", "Dr. New", "specialty", "Cardiology",
                "availableDays", java.util.List.of("FUNDAY"), "startTime", "13:00", "endTime", "15:00",
                "slotMinutes", 20))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/doctors/" + id, admin, Map.of("name", "Dr. New", "specialty", "Cardiology",
                "availableDays", java.util.List.of("FRIDAY"), "startTime", "15:00", "endTime", "13:00",
                "slotMinutes", 20))).andExpect(status().isBadRequest());
        assertThat(doctors.findById(id).orElseThrow().isActive()).isFalse();
    }
}

package com.healthcare.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Role;
import com.healthcare.entity.User;
import com.healthcare.repository.AppointmentRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientProfileRepository;
import com.healthcare.repository.PredictionRepository;
import com.healthcare.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    public static final String PASSWORD = "Passw0rd123";
    public static final String COOKIE = "hda_token";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected PatientProfileRepository profiles;
    @Autowired protected PredictionRepository predictions;
    @Autowired protected AppointmentRepository appointments;
    @Autowired protected DoctorRepository doctors;
    @Autowired protected PasswordEncoder encoder;

    @DynamicPropertySource
    static void aiProps(DynamicPropertyRegistry registry) {
        registry.add("app.ai.base-url", () -> FakeAiServer.INSTANCE.baseUrl());
    }

    @BeforeEach
    void cleanDatabase() {
        FakeAiServer.INSTANCE.reset();
        appointments.deleteAll();
        predictions.deleteAll();
        profiles.deleteAll();
        doctors.deleteAll();
        users.deleteAll();
    }

    // ---- helpers ---------------------------------------------------------
    protected Cookie register(String name, String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register", null,
                Map.of("fullName", name, "email", email, "password", PASSWORD))).andExpect(status().isCreated())
                .andReturn();
        return r.getResponse().getCookie(COOKIE);
    }

    protected Cookie adminCookie() throws Exception {
        User admin = new User();
        admin.setFullName("Admin");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(encoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        users.save(admin);
        MvcResult r = mvc.perform(post("/api/auth/login", null,
                Map.of("email", "admin@example.com", "password", PASSWORD))).andExpect(status().isOk()).andReturn();
        return r.getResponse().getCookie(COOKIE);
    }

    protected Doctor saveDoctor(String name, String days) {
        Doctor d = new Doctor();
        d.setName(name);
        d.setSpecialty("General Medicine");
        d.setAvailableDays(days);
        d.setStartTime(LocalTime.of(9, 0));
        d.setEndTime(LocalTime.of(17, 0));
        d.setSlotMinutes(30);
        d.setActive(true);
        return doctors.save(d);
    }

    protected static final String ALL_DAYS = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

    /** JSON request. A cookie-authenticated unsafe request must carry X-Requested-With (CSRF defence). */
    protected MockHttpServletRequestBuilder post(String url, Cookie cookie, Object body) throws Exception {
        return withBody(MockMvcRequestBuilders.post(url), cookie, body);
    }

    protected MockHttpServletRequestBuilder put(String url, Cookie cookie, Object body) throws Exception {
        return withBody(MockMvcRequestBuilders.put(url), cookie, body);
    }

    protected MockHttpServletRequestBuilder patch(String url, Cookie cookie, Object body) throws Exception {
        return withBody(MockMvcRequestBuilders.patch(url), cookie, body);
    }

    protected MockHttpServletRequestBuilder get(String url, Cookie cookie) {
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.get(url);
        return cookie == null ? b : b.cookie(cookie);
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder b, Cookie cookie, Object body)
            throws Exception {
        b.header("X-Requested-With", "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.content(json.writeValueAsString(body));
        }
        return cookie == null ? b : b.cookie(cookie);
    }
}

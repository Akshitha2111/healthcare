package com.healthcare.config;

import com.healthcare.entity.Doctor;
import com.healthcare.entity.Role;
import com.healthcare.entity.User;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.UserRepository;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD (no credentials live in the repo)
 * and, only when SEED_DEMO_DOCTORS=true and the table is empty, inserts three FICTIONAL demo doctors.
 */
@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppProperties props;
    private final UserRepository users;
    private final DoctorRepository doctors;
    private final PasswordEncoder encoder;

    public DataInitializer(AppProperties props, UserRepository users, DoctorRepository doctors,
                           PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.doctors = doctors;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        String password = props.admin().password();
        if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
            String normalized = email.trim().toLowerCase();
            if (password.length() < 8) {
                throw new IllegalStateException("ADMIN_PASSWORD must be at least 8 characters");
            }
            if (!users.existsByEmail(normalized)) {
                User admin = new User();
                admin.setFullName(props.admin().fullName() == null || props.admin().fullName().isBlank()
                        ? "System Administrator" : props.admin().fullName());
                admin.setEmail(normalized);
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole(Role.ADMIN);
                users.save(admin);
                log.info("Administrator account created from environment configuration");
            }
        }
        if (props.seed().demoDoctors() && doctors.count() == 0) {
            doctors.save(doctor("Dr. Asha Verma (demo)", "General Medicine",
                    "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY", LocalTime.of(9, 0), LocalTime.of(13, 0), 30));
            doctors.save(doctor("Dr. Rohan Iyer (demo)", "Internal Medicine",
                    "MONDAY,WEDNESDAY,FRIDAY", LocalTime.of(14, 0), LocalTime.of(18, 0), 30));
            doctors.save(doctor("Dr. Meera Nair (demo)", "Family Medicine",
                    "TUESDAY,THURSDAY,SATURDAY", LocalTime.of(10, 0), LocalTime.of(14, 0), 20));
            log.info("Seeded fictional demo doctors");
        }
    }

    private static Doctor doctor(String name, String specialty, String days, LocalTime start, LocalTime end,
                                 int slot) {
        Doctor d = new Doctor();
        d.setName(name);
        d.setSpecialty(specialty);
        d.setAvailableDays(days);
        d.setStartTime(start);
        d.setEndTime(end);
        d.setSlotMinutes(slot);
        d.setActive(true);
        return d;
    }
}

package com.healthcare.service;

import com.healthcare.dto.AuthDtos.LoginRequest;
import com.healthcare.dto.AuthDtos.RegisterRequest;
import com.healthcare.dto.AuthDtos.UserDto;
import com.healthcare.entity.PatientProfile;
import com.healthcare.entity.Role;
import com.healthcare.entity.User;
import com.healthcare.exception.ApiException;
import com.healthcare.repository.PatientProfileRepository;
import com.healthcare.repository.UserRepository;
import com.healthcare.security.JwtService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record AuthResult(UserDto user, String token) {}

    private final UserRepository users;
    private final PatientProfileRepository profiles;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    /** Compared against when the e-mail is unknown, so response time does not reveal which e-mails exist. */
    private final String dummyHash;

    public AuthService(UserRepository users, PatientProfileRepository profiles, PasswordEncoder encoder,
                       JwtService jwtService) {
        this.users = users;
        this.profiles = profiles;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResult register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.PATIENT); // self-registration can only ever create patients
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) { // concurrent registration with the same e-mail
            throw ApiException.conflict("An account with this email already exists");
        }
        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profiles.save(profile);
        return new AuthResult(toDto(user), jwtService.generate(user));
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email())).orElse(null);
        boolean matches = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return new AuthResult(toDto(user), jwtService.generate(user));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return users.findById(userId).map(AuthService::toDto)
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getCreatedAt());
    }
}

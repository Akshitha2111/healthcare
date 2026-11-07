package com.healthcare.controller;

import com.healthcare.dto.PatientDtos.ProfileDto;
import com.healthcare.dto.PatientDtos.SummaryDto;
import com.healthcare.dto.PatientDtos.UpdateProfileRequest;
import com.healthcare.security.AuthUser;
import com.healthcare.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/me")
public class PatientController {
    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileDto profile(@AuthenticationPrincipal AuthUser user) {
        return service.profile(user.id());
    }

    @PutMapping
    public ProfileDto update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest req) {
        return service.update(user.id(), req);
    }

    @GetMapping("/summary")
    public SummaryDto summary(@AuthenticationPrincipal AuthUser user) {
        return service.summary(user.id());
    }
}

package com.healthcare.controller;

import com.healthcare.dto.AppointmentDtos.AppointmentDto;
import com.healthcare.dto.AppointmentDtos.BookRequest;
import com.healthcare.dto.CommonDtos.PageResponse;
import com.healthcare.entity.AppointmentStatus;
import com.healthcare.security.AuthUser;
import com.healthcare.service.AppointmentService;
import com.healthcare.service.PageUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping("/my")
    public PageResponse<AppointmentDto> mine(@AuthenticationPrincipal AuthUser user,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) AppointmentStatus status,
                                             @RequestParam(defaultValue = "false") boolean upcoming) {
        Sort sort = upcoming ? Sort.by("appointmentDate").ascending().and(Sort.by("appointmentTime").ascending())
                : Sort.by("appointmentDate").descending().and(Sort.by("appointmentTime").descending());
        return service.mine(user.id(), status, upcoming, PageUtil.of(page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AppointmentDto> book(@AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody BookRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.book(user.id(), req));
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentDto cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.cancel(user.id(), id);
    }
}

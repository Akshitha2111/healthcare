package com.healthcare.controller;

import com.healthcare.dto.CommonDtos.PageResponse;
import com.healthcare.dto.PredictionDtos.CreatePredictionRequest;
import com.healthcare.dto.PredictionDtos.PredictionDto;
import com.healthcare.dto.PredictionDtos.PredictionSummaryDto;
import com.healthcare.security.AuthUser;
import com.healthcare.service.PageUtil;
import com.healthcare.service.PredictionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService service;

    public PredictionController(PredictionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PredictionDto> create(@AuthenticationPrincipal AuthUser user,
                                                @Valid @RequestBody CreatePredictionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.id(), req.symptoms()));
    }

    @GetMapping("/my")
    public PageResponse<PredictionSummaryDto> mine(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "desc") String sort) {
        Sort order = "asc".equalsIgnoreCase(sort) ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        return service.history(user.id(), q, from, to, PageUtil.of(page, size, order));
    }

    @GetMapping("/{id}")
    public PredictionDto get(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.get(user.id(), id);
    }
}

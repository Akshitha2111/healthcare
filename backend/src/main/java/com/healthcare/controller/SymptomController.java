package com.healthcare.controller;

import com.healthcare.dto.PredictionDtos.SymptomCatalogDto;
import com.healthcare.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/symptoms")
public class SymptomController {
    private final PredictionService service;

    public SymptomController(PredictionService service) {
        this.service = service;
    }

    /** The controlled vocabulary the model was trained on (fetched from the AI service). */
    @GetMapping
    public SymptomCatalogDto symptoms() {
        return service.symptomCatalog();
    }
}

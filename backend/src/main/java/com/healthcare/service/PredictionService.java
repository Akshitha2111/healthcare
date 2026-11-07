package com.healthcare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.client.AiClient;
import com.healthcare.client.AiPredictResponse;
import com.healthcare.config.AppProperties;
import com.healthcare.dto.CommonDtos.PageResponse;
import com.healthcare.dto.PredictionDtos.ExplanationDto;
import com.healthcare.dto.PredictionDtos.Influence;
import com.healthcare.dto.PredictionDtos.PredictionDto;
import com.healthcare.dto.PredictionDtos.PredictionSummaryDto;
import com.healthcare.dto.PredictionDtos.ResultItem;
import com.healthcare.dto.PredictionDtos.SymptomCatalogDto;
import com.healthcare.dto.PredictionDtos.SymptomItem;
import com.healthcare.entity.Prediction;
import com.healthcare.entity.PredictionSymptom;
import com.healthcare.exception.ApiException;
import com.healthcare.repository.PredictionRepository;
import com.healthcare.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PredictionService {
    private static final Instant FAR_FUTURE = Instant.parse("9000-01-01T00:00:00Z");

    /** The AI details we keep so that history pages can show the same guidance as the original result. */
    public record StoredDetails(String description, List<String> generalGuidance, String urgentCareNotice,
                                List<String> limitations, ExplanationDto explanation) {}

    private final AiClient ai;
    private final PredictionRepository predictions;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final ZoneId zone;

    public PredictionService(AiClient ai, PredictionRepository predictions, UserRepository users,
                             ObjectMapper mapper, PlatformTransactionManager txManager, AppProperties props) {
        this.ai = ai;
        this.predictions = predictions;
        this.users = users;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
        this.zone = ZoneId.of(props.clinicTimezone());
    }

    public SymptomCatalogDto symptomCatalog() {
        var catalog = ai.symptoms();
        List<SymptomItem> items = catalog.symptoms().stream()
                .map(s -> new SymptomItem(s.name(), s.label())).toList();
        return new SymptomCatalogDto(items.size(), catalog.minSymptoms(), catalog.maxSymptoms(), items);
    }

    /**
     * Not @Transactional on purpose: the (slow, fallible) HTTP call to Flask must not hold a database
     * transaction open. Only the persistence step runs inside a transaction.
     */
    public PredictionDto create(Long userId, List<String> symptoms) {
        AiPredictResponse result = ai.predict(symptoms.stream().map(String::trim).toList());
        return tx.execute(status -> {
            Prediction p = new Prediction();
            p.setPatient(users.getReferenceById(userId));
            p.setPredictedClass(result.predictions().get(0).disease());
            p.setModelName(nullSafe(result.modelName(), "unknown"));
            p.setModelVersion(nullSafe(result.modelVersion(), "unknown"));
            p.setScoreType(nullSafe(result.scoreType(), "model_score"));
            p.setDisclaimer(nullSafe(result.disclaimer(),
                    "This is an educational prediction, not a medical diagnosis."));
            p.setDetailsJson(serializeDetails(result));
            for (String s : result.symptomsUsed()) {
                p.addSymptom(s);
            }
            for (var r : result.predictions()) {
                p.addResult(r.rank(), r.disease(), r.score());
            }
            predictions.save(p);
            return toDto(p);
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<PredictionSummaryDto> history(Long userId, String q, LocalDate from, LocalDate to,
                                                      Pageable pageable) {
        String term = q == null ? "" : q.trim().replaceAll("[%_\\\\]", "");
        if (term.length() > 60) {
            term = term.substring(0, 60);
        }
        Instant fromInstant = from == null ? Instant.EPOCH : from.atStartOfDay(zone).toInstant();
        Instant toInstant = to == null ? FAR_FUTURE : to.plusDays(1).atStartOfDay(zone).toInstant();
        Page<Prediction> page = predictions.search(userId, term, fromInstant, toInstant, pageable);
        return PageResponse.of(page.map(this::toSummary));
    }

    /** Ownership is enforced in the query; someone else's id behaves exactly like a missing id (404). */
    @Transactional(readOnly = true)
    public PredictionDto get(Long userId, Long id) {
        return predictions.findByIdAndPatientId(id, userId).map(this::toDto)
                .orElseThrow(() -> ApiException.notFound("Prediction not found"));
    }

    private PredictionSummaryDto toSummary(Prediction p) {
        Double top = p.getResults().isEmpty() ? null : p.getResults().get(0).getScore();
        return new PredictionSummaryDto(p.getId(), p.getCreatedAt(), p.getPredictedClass(), top,
                p.getSymptoms().stream().map(PredictionSymptom::getName).toList(), p.getModelVersion());
    }

    private PredictionDto toDto(Prediction p) {
        StoredDetails d = readDetails(p.getDetailsJson());
        return new PredictionDto(p.getId(), p.getCreatedAt(), p.getPredictedClass(), p.getModelName(),
                p.getModelVersion(), p.getScoreType(),
                p.getSymptoms().stream().map(s -> new SymptomItem(s.getName(), Labels.symptom(s.getName()))).toList(),
                p.getResults().stream().map(r -> new ResultItem(r.getResultRank(), r.getPredictedClass(), r.getScore()))
                        .toList(),
                d.description(), d.generalGuidance(), d.urgentCareNotice(), d.limitations(), d.explanation(),
                p.getDisclaimer());
    }

    private String serializeDetails(AiPredictResponse r) {
        ExplanationDto explanation = null;
        if (r.explanation() != null) {
            List<Influence> influences = r.explanation().influentialSymptoms() == null ? List.of()
                    : r.explanation().influentialSymptoms().stream()
                    .map(i -> new Influence(i.symptom(), i.label(), i.influence())).toList();
            explanation = new ExplanationDto(r.explanation().method(), influences, r.explanation().caveat());
        }
        try {
            return mapper.writeValueAsString(new StoredDetails(r.description(), r.generalGuidance(),
                    r.urgentCareNotice(), r.limitations(), explanation));
        } catch (JsonProcessingException e) {
            throw ApiException.badGateway("Could not store the prediction details");
        }
    }

    private StoredDetails readDetails(String json) {
        if (json == null || json.isBlank()) {
            return new StoredDetails(null, List.of(), null, List.of(), null);
        }
        try {
            return mapper.readValue(json, StoredDetails.class);
        } catch (JsonProcessingException e) {
            return new StoredDetails(null, List.of(), null, List.of(), null);
        }
    }

    private static String nullSafe(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v;
    }
}

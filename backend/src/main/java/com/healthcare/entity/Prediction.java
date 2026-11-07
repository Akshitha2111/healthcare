package com.healthcare.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "predictions")
public class Prediction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "predicted_class", nullable = false, length = 120)
    private String predictedClass;

    @Column(name = "model_name", nullable = false, length = 60)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 30)
    private String modelVersion;

    @Column(name = "score_type", nullable = false, length = 60)
    private String scoreType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    /** JSON with description, guidance, limitations and explanation returned by the AI service. */
    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    @BatchSize(size = 50)
    private List<PredictionSymptom> symptoms = new ArrayList<>();

    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("resultRank ASC")
    @BatchSize(size = 50)
    private List<PredictionResult> results = new ArrayList<>();

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public void addSymptom(String name) {
        PredictionSymptom s = new PredictionSymptom();
        s.setPrediction(this);
        s.setName(name);
        symptoms.add(s);
    }

    public void addResult(int rank, String disease, double score) {
        PredictionResult r = new PredictionResult();
        r.setPrediction(this);
        r.setResultRank(rank);
        r.setPredictedClass(disease);
        r.setScore(score);
        results.add(r);
    }

    public Long getId() { return id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public String getPredictedClass() { return predictedClass; }
    public void setPredictedClass(String predictedClass) { this.predictedClass = predictedClass; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public List<PredictionSymptom> getSymptoms() { return symptoms; }
    public List<PredictionResult> getResults() { return results; }
}

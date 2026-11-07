package com.healthcare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prediction_symptoms")
public class PredictionSymptom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @Column(name = "symptom_name", nullable = false, length = 80)
    private String name;

    public Long getId() { return id; }
    public Prediction getPrediction() { return prediction; }
    public void setPrediction(Prediction prediction) { this.prediction = prediction; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

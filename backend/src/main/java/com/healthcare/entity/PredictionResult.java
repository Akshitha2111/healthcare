package com.healthcare.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prediction_results")
public class PredictionResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @Column(name = "predicted_class", nullable = false, length = 120)
    private String predictedClass;

    @Column(nullable = false)
    private double score;

    @Column(name = "result_rank", nullable = false)
    private int resultRank;

    public Long getId() { return id; }
    public Prediction getPrediction() { return prediction; }
    public void setPrediction(Prediction prediction) { this.prediction = prediction; }
    public String getPredictedClass() { return predictedClass; }
    public void setPredictedClass(String predictedClass) { this.predictedClass = predictedClass; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getResultRank() { return resultRank; }
    public void setResultRank(int resultRank) { this.resultRank = resultRank; }
}

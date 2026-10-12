package com.example.fakenews.web;

import java.util.List;

public record PredictionResponse(
        String predictedClass,
        double confidence,
        double probabilityFake,
        List<InfluentialTerm> influentialTerms) {
    public record InfluentialTerm(String term, double tfidfWeight) {}
}

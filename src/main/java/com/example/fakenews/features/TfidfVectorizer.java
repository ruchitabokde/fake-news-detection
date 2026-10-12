package com.example.fakenews.features;

import com.example.fakenews.preprocessing.TextPreprocessor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TfidfVectorizer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final int MAX_FEATURES = 20_000;

    private final Map<String, Integer> vocabulary = new LinkedHashMap<>();
    private final List<String> terms = new ArrayList<>();
    private final List<Double> inverseDocumentFrequencies = new ArrayList<>();

    public void fit(List<String> documents, TextPreprocessor preprocessor) {
        Map<String, Integer> documentFrequencies = new HashMap<>();
        for (String document : documents) {
            Set<String> documentTerms = new LinkedHashSet<>(terms(preprocessor.process(document)));
            documentTerms.forEach(term -> documentFrequencies.merge(term, 1, Integer::sum));
        }

        List<String> selectedTerms = documentFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_FEATURES)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (selectedTerms.isEmpty()) {
            throw new IllegalArgumentException("No usable terms were found in the training documents.");
        }

        vocabulary.clear();
        terms.clear();
        inverseDocumentFrequencies.clear();
        for (String term : selectedTerms) {
            vocabulary.put(term, terms.size());
            terms.add(term);
            double idf = Math.log((1.0 + documents.size()) / (1.0 + documentFrequencies.get(term))) + 1.0;
            inverseDocumentFrequencies.add(idf);
        }
    }

    public double[] transform(String document, TextPreprocessor preprocessor) {
        if (terms.isEmpty()) {
            throw new IllegalStateException("Fit the TF-IDF vectorizer before transforming documents.");
        }
        double[] vector = new double[terms.size()];
        for (String term : terms(preprocessor.process(document))) {
            Integer index = vocabulary.get(term);
            if (index != null) {
                vector[index] += inverseDocumentFrequencies.get(index);
            }
        }

        double magnitude = 0.0;
        for (double value : vector) {
            magnitude += value * value;
        }
        if (magnitude > 0.0) {
            magnitude = Math.sqrt(magnitude);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
        return vector;
    }

    public List<WeightedTerm> prominentTerms(double[] vector, int limit) {
        List<WeightedTerm> ranked = new ArrayList<>();
        for (int i = 0; i < vector.length; i++) {
            if (vector[i] > 0.0) {
                ranked.add(new WeightedTerm(terms.get(i), vector[i]));
            }
        }
        ranked.sort(Comparator.comparingDouble(WeightedTerm::weight).reversed());
        return ranked.stream().limit(limit).toList();
    }

    public int size() {
        return terms.size();
    }

    public List<String> attributeNames() {
        return List.copyOf(terms);
    }

    private List<String> terms(List<String> tokens) {
        List<String> result = new ArrayList<>(tokens.size() * 2);
        for (int i = 0; i < tokens.size(); i++) {
            result.add(tokens.get(i));
            if (i + 1 < tokens.size()) {
                result.add(tokens.get(i) + " " + tokens.get(i + 1));
            }
        }
        return result;
    }

    public record WeightedTerm(String term, double weight) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}

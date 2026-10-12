package com.example.fakenews.training;

import com.example.fakenews.features.TfidfVectorizer;
import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.Serial;
import java.io.Serializable;

public record ModelBundle(
        String modelName,
        Classifier classifier,
        TfidfVectorizer vectorizer,
        Instances header) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}

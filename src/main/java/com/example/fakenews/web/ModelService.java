package com.example.fakenews.web;

import com.example.fakenews.preprocessing.TextPreprocessor;
import com.example.fakenews.training.ModelBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import weka.core.DenseInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@Lazy
public class ModelService {
    private final ModelBundle bundle;
    private final TextPreprocessor preprocessor;

    public ModelService(
            @Value("${app.model-path:models/fake-news.model}") String modelPath,
            TextPreprocessor preprocessor) throws Exception {
        Path path = Path.of(modelPath);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Trained model not found at " + path.toAbsolutePath()
                    + ". Train the model first using the README instructions.");
        }
        this.bundle = (ModelBundle) weka.core.SerializationHelper.read(path.toString());
        this.preprocessor = preprocessor;
    }

    public PredictionResponse predict(String text) throws Exception {
        double[] vector = bundle.vectorizer().transform(text, preprocessor);
        double[] values = new double[vector.length + 1];
        System.arraycopy(vector, 0, values, 0, vector.length);
        values[vector.length] = Double.NaN;
        var instance = new DenseInstance(1.0, values);
        instance.setDataset(bundle.header());
        double[] distribution = bundle.classifier().distributionForInstance(instance);
        double probabilityFake = distribution.length > 1 ? distribution[1] : 0.0;
        boolean fake = probabilityFake >= 0.5;
        double confidence = fake ? probabilityFake : 1.0 - probabilityFake;
        List<PredictionResponse.InfluentialTerm> terms = new ArrayList<>();
        for (var term : bundle.vectorizer().prominentTerms(vector, 8)) {
            terms.add(new PredictionResponse.InfluentialTerm(term.term(), term.weight()));
        }
        return new PredictionResponse(fake ? "fake" : "real", confidence, probabilityFake, terms);
    }

    public String modelName() {
        return bundle.modelName();
    }
}

package com.example.fakenews.training;

import com.example.fakenews.data.CsvDatasetLoader;
import com.example.fakenews.data.NewsArticle;
import com.example.fakenews.evaluation.ClassificationMetrics;
import com.example.fakenews.features.TfidfVectorizer;
import com.example.fakenews.preprocessing.TextPreprocessor;
import org.springframework.stereotype.Component;
import weka.classifiers.Classifier;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class ModelTrainer {
    private static final int RANDOM_SEED = 42;
    private final CsvDatasetLoader loader;
    private final TextPreprocessor preprocessor;

    public ModelTrainer(CsvDatasetLoader loader, TextPreprocessor preprocessor) {
        this.loader = loader;
        this.preprocessor = preprocessor;
    }

    public void train(Path datasetPath, Path modelPath) throws Exception {
        List<NewsArticle> articles = loader.load(datasetPath);
        List<NewsArticle> training = new ArrayList<>();
        List<NewsArticle> testing = new ArrayList<>();
        stratifiedSplit(articles, training, testing);
        System.out.printf("Loaded %d articles; training on %d, evaluating on %d.%n",
                articles.size(), training.size(), testing.size());

        Candidate best = null;
        for (String name : List.of("Logistic Regression", "Random Forest", "Support Vector Machine")) {
            Candidate candidate = evaluate(name, training, testing);
            printMetrics(candidate.name(), candidate.metrics());
            if (best == null || candidate.metrics().fakeF1() > best.metrics().fakeF1()) {
                best = candidate;
            }
        }

        TfidfVectorizer finalVectorizer = new TfidfVectorizer();
        finalVectorizer.fit(articles.stream().map(NewsArticle::content).toList(), preprocessor);
        Instances fullTrainingData = toInstances(articles, finalVectorizer);
        Classifier finalClassifier = classifier(best.name());
        finalClassifier.buildClassifier(fullTrainingData);
        Path parent = modelPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        weka.core.SerializationHelper.write(modelPath.toString(),
                new ModelBundle(best.name(), finalClassifier, finalVectorizer, new Instances(fullTrainingData, 0)));
        System.out.printf("Selected %s by fake-class F1 (%.4f); saved model to %s (%d features).%n",
                best.name(), best.metrics().fakeF1(), modelPath.toAbsolutePath(), finalVectorizer.size());
    }

    private Candidate evaluate(String name, List<NewsArticle> training, List<NewsArticle> testing)
            throws Exception {
        TfidfVectorizer vectorizer = new TfidfVectorizer();
        vectorizer.fit(training.stream().map(NewsArticle::content).toList(), preprocessor);
        Instances trainData = toInstances(training, vectorizer);
        Instances testData = toInstances(testing, vectorizer);
        Classifier classifier = classifier(name);
        classifier.buildClassifier(trainData);

        boolean[] actual = new boolean[testData.numInstances()];
        boolean[] predicted = new boolean[testData.numInstances()];
        for (int i = 0; i < testData.numInstances(); i++) {
            actual[i] = testData.instance(i).classValue() == 1.0;
            predicted[i] = classifier.classifyInstance(testData.instance(i)) == 1.0;
        }
        return new Candidate(name, ClassificationMetrics.from(actual, predicted));
    }

    private Instances toInstances(List<NewsArticle> articles, TfidfVectorizer vectorizer) {
        ArrayList<Attribute> attributes = new ArrayList<>(vectorizer.size() + 1);
        for (var term : vectorizerTerms(vectorizer)) {
            attributes.add(new Attribute(term));
        }
        attributes.add(new Attribute("label", List.of("real", "fake")));
        Instances instances = new Instances("fake-news", attributes, articles.size());
        instances.setClassIndex(attributes.size() - 1);
        for (NewsArticle article : articles) {
            double[] values = vectorizer.transform(article.content(), preprocessor);
            double[] instanceValues = new double[values.length + 1];
            System.arraycopy(values, 0, instanceValues, 0, values.length);
            instanceValues[values.length] = article.fake() ? 1.0 : 0.0;
            instances.add(new SparseInstance(1.0, instanceValues));
        }
        return instances;
    }

    private List<String> vectorizerTerms(TfidfVectorizer vectorizer) {
        return vectorizer.attributeNames();
    }

    private Classifier classifier(String name) {
        return switch (name) {
            case "Logistic Regression" -> {
                Logistic logistic = new Logistic();
                logistic.setMaxIts(200);
                yield logistic;
            }
            case "Random Forest" -> {
                RandomForest forest = new RandomForest();
                forest.setNumIterations(100);
                forest.setSeed(RANDOM_SEED);
                yield forest;
            }
            case "Support Vector Machine" -> {
                SMO smo = new SMO();
                smo.setBuildCalibrationModels(true);
                yield smo;
            }
            default -> throw new IllegalArgumentException("Unknown model: " + name);
        };
    }

    private void stratifiedSplit(List<NewsArticle> articles, List<NewsArticle> training,
                                 List<NewsArticle> testing) throws IOException {
        List<NewsArticle> real = new ArrayList<>();
        List<NewsArticle> fake = new ArrayList<>();
        for (NewsArticle article : articles) (article.fake() ? fake : real).add(article);
        if (real.size() < 2 || fake.size() < 2) {
            throw new IOException("At least two real and two fake rows are required for a stratified evaluation.");
        }
        Random random = new Random(RANDOM_SEED);
        splitClass(real, training, testing, random);
        splitClass(fake, training, testing, random);
        Collections.shuffle(training, random);
        Collections.shuffle(testing, random);
    }

    private void splitClass(List<NewsArticle> group, List<NewsArticle> training,
                            List<NewsArticle> testing, Random random) {
        Collections.shuffle(group, random);
        int testCount = Math.max(1, (int) Math.round(group.size() * 0.2));
        testCount = Math.min(testCount, group.size() - 1);
        testing.addAll(group.subList(0, testCount));
        training.addAll(group.subList(testCount, group.size()));
    }

    private void printMetrics(String name, ClassificationMetrics metrics) {
        System.out.printf("%n%s%n  Accuracy: %.4f  Fake precision: %.4f  Fake recall: %.4f  Fake F1: %.4f%n"
                        + "  Confusion matrix [actual rows real/fake, predicted columns real/fake]: "
                        + "[[%d, %d], [%d, %d]]%n",
                name, metrics.accuracy(), metrics.fakePrecision(), metrics.fakeRecall(), metrics.fakeF1(),
                metrics.trueNegative(), metrics.falsePositive(), metrics.falseNegative(), metrics.truePositive());
    }

    private record Candidate(String name, ClassificationMetrics metrics) {}
}

package com.example.fakenews.evaluation;

public record ClassificationMetrics(
        double accuracy,
        double fakePrecision,
        double fakeRecall,
        double fakeF1,
        long trueNegative,
        long falsePositive,
        long falseNegative,
        long truePositive) {

    public static ClassificationMetrics from(boolean[] actualFake, boolean[] predictedFake) {
        if (actualFake.length != predictedFake.length || actualFake.length == 0) {
            throw new IllegalArgumentException("Actual and predicted labels must have the same non-zero length.");
        }
        long tn = 0, fp = 0, fn = 0, tp = 0;
        for (int i = 0; i < actualFake.length; i++) {
            if (actualFake[i] && predictedFake[i]) tp++;
            else if (actualFake[i]) fn++;
            else if (predictedFake[i]) fp++;
            else tn++;
        }
        double precision = ratio(tp, tp + fp);
        double recall = ratio(tp, tp + fn);
        double f1 = ratio(2 * precision * recall, precision + recall);
        return new ClassificationMetrics(
                ratio(tp + tn, actualFake.length), precision, recall, f1, tn, fp, fn, tp);
    }

    private static double ratio(double numerator, double denominator) {
        return denominator == 0 ? 0.0 : numerator / denominator;
    }
}

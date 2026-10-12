package com.example.fakenews.evaluation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificationMetricsTest {
    @Test
    void computesFakeClassMetricsAndConfusionMatrix() {
        ClassificationMetrics metrics = ClassificationMetrics.from(
                new boolean[]{false, false, true, true},
                new boolean[]{false, true, true, false});

        assertEquals(0.5, metrics.accuracy());
        assertEquals(0.5, metrics.fakePrecision());
        assertEquals(0.5, metrics.fakeRecall());
        assertEquals(0.5, metrics.fakeF1());
        assertEquals(1, metrics.trueNegative());
        assertEquals(1, metrics.falsePositive());
        assertEquals(1, metrics.falseNegative());
        assertEquals(1, metrics.truePositive());
    }
}

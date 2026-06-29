package com.example.imagequality;

import java.util.Collections;
import java.util.List;

public class QualityResult {
    private final int overallScore;
    private final String summary;
    private final List<MetricResult> metrics;
    private final long elapsedMs;

    public QualityResult(int overallScore, String summary, List<MetricResult> metrics, long elapsedMs) {
        this.overallScore = overallScore;
        this.summary = summary;
        this.metrics = Collections.unmodifiableList(metrics);
        this.elapsedMs = elapsedMs;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public List<MetricResult> getMetrics() {
        return metrics;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}


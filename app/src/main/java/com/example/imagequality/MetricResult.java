package com.example.imagequality;

public class MetricResult {
    private final String name;
    private final int score;
    private final String rawValue;
    private final String explanation;

    public MetricResult(String name, int score, String rawValue, String explanation) {
        this.name = name;
        this.score = score;
        this.rawValue = rawValue;
        this.explanation = explanation;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getExplanation() {
        return explanation;
    }
}


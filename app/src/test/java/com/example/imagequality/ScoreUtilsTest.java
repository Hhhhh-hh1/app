package com.example.imagequality;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScoreUtilsTest {
    @Test
    public void clampScoreKeepsValueInsideRange() {
        assertEquals(0, ScoreUtils.clampScore(-8.0));
        assertEquals(56, ScoreUtils.clampScore(55.6));
        assertEquals(100, ScoreUtils.clampScore(120.0));
    }

    @Test
    public void normalizeMapsLinearRangeToScore() {
        assertEquals(0.0, ScoreUtils.normalize(5.0, 10.0, 20.0), 0.001);
        assertEquals(50.0, ScoreUtils.normalize(15.0, 10.0, 20.0), 0.001);
        assertEquals(100.0, ScoreUtils.normalize(25.0, 10.0, 20.0), 0.001);
    }

    @Test
    public void weightedOverallUsesExpectedWeights() {
        int score = ScoreUtils.weightedOverall(80, 70, 60, 50, 40);
        assertEquals(65, score);
    }
}


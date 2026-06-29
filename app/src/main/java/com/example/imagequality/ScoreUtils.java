package com.example.imagequality;

public final class ScoreUtils {
    private ScoreUtils() {
    }

    public static int clampScore(double value) {
        return (int) Math.round(clamp(value, 0.0, 100.0));
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static double normalize(double value, double low, double high) {
        if (high <= low) {
            return 0.0;
        }
        return clamp((value - low) * 100.0 / (high - low), 0.0, 100.0);
    }

    public static double normalizeLog(double value, double low, double high) {
        double v = Math.log10(Math.max(0.0, value) + 1.0);
        double l = Math.log10(Math.max(0.0, low) + 1.0);
        double h = Math.log10(Math.max(0.0, high) + 1.0);
        return normalize(v, l, h);
    }

    public static int weightedOverall(int sharpness, int exposure, int noise, int contrast, int color, int colorfulness, int entropy) {
        return clampScore(sharpness * 0.25
                + exposure * 0.20
                + noise * 0.15
                + contrast * 0.15
                + color * 0.10
                + colorfulness * 0.08
                + entropy * 0.07);
    }
}


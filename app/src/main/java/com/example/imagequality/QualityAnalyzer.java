package com.example.imagequality;

import android.graphics.Bitmap;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QualityAnalyzer {
    private static final int MAX_ANALYSIS_EDGE = 1024;

    public QualityResult analyze(Bitmap source) {
        long start = SystemClock.elapsedRealtime();
        Bitmap bitmap = prepareAnalysisBitmap(source);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        double[] luminance = new double[count];
        int[] histogram = new int[256];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        double sumY = 0.0;
        double sumY2 = 0.0;
        double sumR = 0.0;
        double sumG = 0.0;
        double sumB = 0.0;
        double sumRg = 0.0;
        double sumRg2 = 0.0;
        double sumYb = 0.0;
        double sumYb2 = 0.0;
        int overCount = 0;
        int underCount = 0;

        for (int i = 0; i < count; i++) {
            int color = pixels[i];
            int r = (color >> 16) & 0xff;
            int g = (color >> 8) & 0xff;
            int b = color & 0xff;
            double y = 0.299 * r + 0.587 * g + 0.114 * b;
            luminance[i] = y;
            int yi = (int) ScoreUtils.clamp(Math.round(y), 0, 255);
            histogram[yi]++;
            sumY += y;
            sumY2 += y * y;
            sumR += r;
            sumG += g;
            sumB += b;

            double rg = r - g;
            double yb = 0.5 * (r + g) - b;
            sumRg += rg;
            sumRg2 += rg * rg;
            sumYb += yb;
            sumYb2 += yb * yb;

            if (yi >= 245) {
                overCount++;
            } else if (yi <= 10) {
                underCount++;
            }
        }

        double meanY = sumY / count;
        double varianceY = Math.max(0.0, sumY2 / count - meanY * meanY);
        double stdY = Math.sqrt(varianceY);
        double overRatio = overCount * 1.0 / count;
        double underRatio = underCount * 1.0 / count;
        int p5 = percentile(histogram, count, 0.05);
        int p95 = percentile(histogram, count, 0.95);
        int dynamicRange = p95 - p5;

        double lapVariance = laplacianVariance(luminance, width, height);
        double noiseStd = estimateNoise(luminance, width, height);
        double avgR = sumR / count;
        double avgG = sumG / count;
        double avgB = sumB / count;
        double maxColorDiff = Math.max(Math.abs(avgR - avgG),
                Math.max(Math.abs(avgR - avgB), Math.abs(avgG - avgB)));

        // 计算色彩丰富度 (Hasler & Suesstrunk Metric)
        double meanRg = sumRg / count;
        double varRg = Math.max(0.0, sumRg2 / count - meanRg * meanRg);
        double meanYb = sumYb / count;
        double varYb = Math.max(0.0, sumYb2 / count - meanYb * meanYb);
        double colorfulness = Math.sqrt(varRg + varYb) + 0.3 * Math.sqrt(meanRg * meanRg + meanYb * meanYb);
        int colorfulnessScore = ScoreUtils.clampScore(ScoreUtils.normalize(colorfulness, 12.0, 65.0));

        // 计算细节丰富度 / 信息熵 (Shannon Entropy)
        double entropy = 0.0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                double p = histogram[i] * 1.0 / count;
                entropy -= p * (Math.log(p) / Math.log(2.0));
            }
        }
        int entropyScore = ScoreUtils.clampScore(ScoreUtils.normalize(entropy, 3.8, 7.2));

        int sharpnessScore = ScoreUtils.clampScore(ScoreUtils.normalizeLog(lapVariance, 20.0, 900.0));
        int exposureScore = ScoreUtils.clampScore(100.0
                - Math.abs(meanY - 128.0) * 0.45
                - overRatio * 120.0
                - underRatio * 110.0);
        int noiseScore = ScoreUtils.clampScore(100.0 - Math.max(0.0, noiseStd - 3.0) * 7.0);
        int contrastScore = ScoreUtils.clampScore(
                ScoreUtils.normalize(dynamicRange, 70.0, 210.0) * 0.55
                        + ScoreUtils.normalize(stdY, 18.0, 70.0) * 0.45);
        int colorScore = ScoreUtils.clampScore(100.0 - Math.max(0.0, maxColorDiff - 8.0) * 1.7);
        
        int overall = ScoreUtils.weightedOverall(
                sharpnessScore, exposureScore, noiseScore, contrastScore, colorScore, colorfulnessScore, entropyScore);

        List<MetricResult> metrics = new ArrayList<>();
        metrics.add(new MetricResult(
                "清晰度 / 模糊检测",
                sharpnessScore,
                String.format(Locale.CHINA, "Laplacian 方差 %.1f", lapVariance),
                explainSharpness(lapVariance, sharpnessScore)
        ));
        metrics.add(new MetricResult(
                "曝光 / 亮度分布",
                exposureScore,
                String.format(Locale.CHINA, "均值 %.1f，过曝 %.2f%%，欠曝 %.2f%%",
                        meanY, overRatio * 100.0, underRatio * 100.0),
                explainExposure(meanY, overRatio, underRatio)
        ));
        metrics.add(new MetricResult(
                "噪点估计",
                noiseScore,
                String.format(Locale.CHINA, "低梯度残差标准差 %.2f", noiseStd),
                explainNoise(noiseStd)
        ));
        metrics.add(new MetricResult(
                "对比度",
                contrastScore,
                String.format(Locale.CHINA, "亮度标准差 %.1f，P95-P5=%d", stdY, dynamicRange),
                explainContrast(stdY, dynamicRange)
        ));
        metrics.add(new MetricResult(
                "色偏",
                colorScore,
                String.format(Locale.CHINA, "RGB 均值差最大 %.1f", maxColorDiff),
                explainColor(maxColorDiff)
        ));
        metrics.add(new MetricResult(
                "色彩丰富度 / 艳丽度",
                colorfulnessScore,
                String.format(Locale.CHINA, "色彩指数 %.1f", colorfulness),
                explainColorfulness(colorfulness)
        ));
        metrics.add(new MetricResult(
                "细节丰富度 / 信息熵",
                entropyScore,
                String.format(Locale.CHINA, "信息熵 %.2f bits", entropy),
                explainEntropy(entropy)
        ));

        long elapsed = SystemClock.elapsedRealtime() - start;
        if (bitmap != source) {
            bitmap.recycle();
        }
        return new QualityResult(overall, buildSummary(overall, metrics), metrics, elapsed);
    }

    private Bitmap prepareAnalysisBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int maxEdge = Math.max(width, height);
        if (maxEdge <= MAX_ANALYSIS_EDGE) {
            return source;
        }
        double scale = MAX_ANALYSIS_EDGE * 1.0 / maxEdge;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }

    private int percentile(int[] histogram, int total, double percentile) {
        int target = (int) Math.ceil(total * percentile);
        int acc = 0;
        for (int i = 0; i < histogram.length; i++) {
            acc += histogram[i];
            if (acc >= target) {
                return i;
            }
        }
        return 255;
    }

    private double laplacianVariance(double[] luminance, int width, int height) {
        double mean = 0.0;
        double m2 = 0.0;
        int n = 0;
        for (int y = 1; y < height - 1; y++) {
            int row = y * width;
            for (int x = 1; x < width - 1; x++) {
                int idx = row + x;
                double value = 4.0 * luminance[idx]
                        - luminance[idx - 1]
                        - luminance[idx + 1]
                        - luminance[idx - width]
                        - luminance[idx + width];
                n++;
                double delta = value - mean;
                mean += delta / n;
                m2 += delta * (value - mean);
            }
        }
        return n > 1 ? m2 / (n - 1) : 0.0;
    }

    private double estimateNoise(double[] luminance, int width, int height) {
        double sum = 0.0;
        double sum2 = 0.0;
        int n = 0;
        for (int y = 1; y < height - 1; y++) {
            int row = y * width;
            for (int x = 1; x < width - 1; x++) {
                int idx = row + x;
                double horizontal = Math.abs(luminance[idx + 1] - luminance[idx - 1]);
                double vertical = Math.abs(luminance[idx + width] - luminance[idx - width]);
                if (horizontal + vertical > 18.0) {
                    continue;
                }
                double neighborMean = (luminance[idx - 1] + luminance[idx + 1]
                        + luminance[idx - width] + luminance[idx + width]) / 4.0;
                double residual = luminance[idx] - neighborMean;
                sum += residual;
                sum2 += residual * residual;
                n++;
            }
        }
        if (n < 20) {
            return 0.0;
        }
        double mean = sum / n;
        return Math.sqrt(Math.max(0.0, sum2 / n - mean * mean));
    }

    private String explainSharpness(double lapVariance, int score) {
        if (score >= 75) {
            return "边缘能量较高，整体清晰。";
        }
        if (score >= 45) {
            return "存在轻度模糊或细节不足，仍可用于一般预览。";
        }
        return "Laplacian 方差偏低，图片可能明显模糊或缺少纹理。";
    }

    private String explainExposure(double meanY, double overRatio, double underRatio) {
        if (overRatio > 0.08) {
            return "高亮区域比例偏高，存在过曝风险。";
        }
        if (underRatio > 0.08) {
            return "暗部像素比例偏高，存在欠曝风险。";
        }
        if (meanY < 85.0) {
            return "整体亮度偏暗，但没有大面积死黑。";
        }
        if (meanY > 175.0) {
            return "整体亮度偏亮，但没有大面积死白。";
        }
        return "亮度分布接近正常，过曝和欠曝比例较低。";
    }

    private String explainNoise(double noiseStd) {
        if (noiseStd < 5.0) {
            return "平坦区域残差较小，噪点较低。";
        }
        if (noiseStd < 9.0) {
            return "平坦区域有一定随机波动，噪点中等。";
        }
        return "平坦区域随机波动偏高，可能存在明显噪点或压缩伪影。";
    }

    private String explainContrast(double stdY, int dynamicRange) {
        if (dynamicRange > 150 && stdY > 40.0) {
            return "亮暗层次较丰富，对比度较好。";
        }
        if (dynamicRange > 95) {
            return "对比度基本可用，但层次不算充分。";
        }
        return "亮度范围较窄，画面可能发灰或缺少层次。";
    }

    private String explainColor(double maxColorDiff) {
        if (maxColorDiff < 12.0) {
            return "RGB 通道均值接近，未发现明显整体色偏。";
        }
        if (maxColorDiff < 28.0) {
            return "存在轻微色偏，可能由光源或白平衡造成。";
        }
        return "RGB 通道差异明显，可能存在较强色偏。";
    }

    private String explainColorfulness(double colorfulness) {
        if (colorfulness < 18.0) {
            return "色彩饱和度极低，画面接近黑白或灰暗褪色。";
        }
        if (colorfulness < 38.0) {
            return "色彩丰富度中等，画面色彩自然适中。";
        }
        return "色彩饱满艳丽，色域分布丰富。";
    }

    private String explainEntropy(double entropy) {
        if (entropy < 4.5) {
            return "信息量极低，画面极单调或有大量纯色死白死黑。";
        }
        if (entropy < 6.8) {
            return "信息量适中，画面包含基本的细节与纹理。";
        }
        return "信息熵极高，细节非常饱满、纹理复杂多样。";
    }

    private String buildSummary(int overall, List<MetricResult> metrics) {
        String level;
        if (overall >= 85) {
            level = "画质优秀";
        } else if (overall >= 70) {
            level = "画质良好";
        } else if (overall >= 55) {
            level = "画质一般";
        } else {
            level = "画质偏差";
        }

        MetricResult weakest = metrics.get(0);
        for (MetricResult metric : metrics) {
            if (metric.getScore() < weakest.getScore()) {
                weakest = metric;
            }
        }
        return level + "。最低子项是“" + weakest.getName() + "”（"
                + weakest.getScore() + " 分），建议优先检查该维度。";
    }
}


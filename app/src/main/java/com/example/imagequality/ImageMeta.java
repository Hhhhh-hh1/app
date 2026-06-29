package com.example.imagequality;

public class ImageMeta {
    private final String displayName;
    private final String mimeType;
    private final String formatName;
    private final int originalWidth;
    private final int originalHeight;
    private final int decodedWidth;
    private final int decodedHeight;
    private final int sampleSize;
    private final long fileSizeBytes;

    public ImageMeta(String displayName, String mimeType, String formatName,
                     int originalWidth, int originalHeight,
                     int decodedWidth, int decodedHeight,
                     int sampleSize, long fileSizeBytes) {
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.formatName = formatName;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.decodedWidth = decodedWidth;
        this.decodedHeight = decodedHeight;
        this.sampleSize = sampleSize;
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFormatName() {
        return formatName;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public int getDecodedWidth() {
        return decodedWidth;
    }

    public int getDecodedHeight() {
        return decodedHeight;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String describe() {
        String sizeText = fileSizeBytes > 0
                ? String.format(java.util.Locale.CHINA, "%.2f MB", fileSizeBytes / 1024.0 / 1024.0)
                : "未知";
        return "文件名：" + displayName
                + "\n格式：" + formatName + "（" + mimeType + "）"
                + "\n原始尺寸：" + originalWidth + " x " + originalHeight
                + "\n分析尺寸：" + decodedWidth + " x " + decodedHeight
                + "\n采样倍率：1/" + sampleSize
                + "\n文件大小：" + sizeText;
    }
}


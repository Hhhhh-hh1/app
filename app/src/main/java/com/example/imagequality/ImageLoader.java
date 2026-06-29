package com.example.imagequality;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Size;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class ImageLoader {
    private static final int MAX_DECODE_EDGE = 2048;

    public LoadedImage load(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String displayName = queryDisplayName(resolver, uri);
        long fileSize = queryFileSize(resolver, uri);
        String mimeType = resolver.getType(uri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "未知";
        }

        Bitmap bitmap;
        int[] originalSize;
        int sampleSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DecodeResult result = decodeWithImageDecoder(resolver, uri);
            bitmap = result.bitmap;
            originalSize = result.originalSize;
            sampleSize = result.sampleSize;
        } else {
            DecodeResult result = decodeWithBitmapFactory(resolver, uri);
            bitmap = result.bitmap;
            originalSize = result.originalSize;
            sampleSize = result.sampleSize;
        }

        if (bitmap == null) {
            throw new IOException("图片解码失败，可能是当前设备不支持该格式。");
        }

        Bitmap analysisBitmap = ensureArgb8888(bitmap);
        ImageMeta meta = new ImageMeta(
                displayName,
                mimeType,
                guessFormat(displayName, mimeType),
                originalSize[0],
                originalSize[1],
                analysisBitmap.getWidth(),
                analysisBitmap.getHeight(),
                sampleSize,
                fileSize
        );
        return new LoadedImage(analysisBitmap, meta);
    }

    private DecodeResult decodeWithImageDecoder(ContentResolver resolver, Uri uri) throws IOException {
        final int[] originalSize = new int[]{0, 0};
        final int[] sample = new int[]{1};
        ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
        try {
            Bitmap bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, source1) -> {
                Size size = info.getSize();
                originalSize[0] = size.getWidth();
                originalSize[1] = size.getHeight();
                sample[0] = calculateSampleSize(size.getWidth(), size.getHeight());
                decoder.setTargetSampleSize(sample[0]);
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            return new DecodeResult(bitmap, originalSize, sample[0]);
        } catch (IOException | RuntimeException ex) {
            throw new IOException("图片解码失败。HEIC/AVIF/GIF 等格式依赖系统版本和设备解码器。", ex);
        }
    }

    private DecodeResult decodeWithBitmapFactory(ContentResolver resolver, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("无法读取图片尺寸，可能是不支持的图片格式。");
        }

        int sample = calculateSampleSize(bounds.outWidth, bounds.outHeight);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream in = resolver.openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        return new DecodeResult(bitmap, new int[]{bounds.outWidth, bounds.outHeight}, sample);
    }

    private Bitmap ensureArgb8888(Bitmap bitmap) {
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            return bitmap;
        }
        Bitmap converted = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        if (converted != bitmap) {
            bitmap.recycle();
        }
        return converted;
    }

    private int calculateSampleSize(int width, int height) {
        int maxEdge = Math.max(width, height);
        int sample = 1;
        while (maxEdge / sample > MAX_DECODE_EDGE) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }

    private String queryDisplayName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // 部分文件提供方不返回 DISPLAY_NAME，后续使用 URI 兜底。
        }
        String last = uri.getLastPathSegment();
        return last == null ? "未命名图片" : last;
    }

    private long queryFileSize(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (RuntimeException ignored) {
            // 文件大小不是核心指标，读取不到时显示未知。
        }
        return -1L;
    }

    private String guessFormat(String displayName, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        if (lowerMime.contains("jpeg") || lowerMime.contains("jpg")) {
            return "JPEG";
        }
        if (lowerMime.contains("png")) {
            return "PNG";
        }
        if (lowerMime.contains("webp")) {
            return "WebP";
        }
        if (lowerMime.contains("heic") || lowerMime.contains("heif")) {
            return "HEIC/HEIF";
        }
        if (lowerMime.contains("avif")) {
            return "AVIF";
        }
        if (lowerMime.contains("gif")) {
            return "GIF";
        }
        if (lowerMime.contains("bmp")) {
            return "BMP";
        }
        String lowerName = displayName == null ? "" : displayName.toLowerCase(Locale.US);
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "JPEG";
        }
        if (lowerName.endsWith(".png")) {
            return "PNG";
        }
        if (lowerName.endsWith(".webp")) {
            return "WebP";
        }
        if (lowerName.endsWith(".heic") || lowerName.endsWith(".heif")) {
            return "HEIC/HEIF";
        }
        if (lowerName.endsWith(".avif")) {
            return "AVIF";
        }
        if (lowerName.endsWith(".gif")) {
            return "GIF";
        }
        if (lowerName.endsWith(".bmp")) {
            return "BMP";
        }
        return "未知格式";
    }

    public static class LoadedImage {
        private final Bitmap bitmap;
        private final ImageMeta meta;

        LoadedImage(Bitmap bitmap, ImageMeta meta) {
            this.bitmap = bitmap;
            this.meta = meta;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public ImageMeta getMeta() {
            return meta;
        }
    }

    private static class DecodeResult {
        private final Bitmap bitmap;
        private final int[] originalSize;
        private final int sampleSize;

        private DecodeResult(Bitmap bitmap, int[] originalSize, int sampleSize) {
            this.bitmap = bitmap;
            this.originalSize = originalSize;
            this.sampleSize = sampleSize;
        }
    }
}


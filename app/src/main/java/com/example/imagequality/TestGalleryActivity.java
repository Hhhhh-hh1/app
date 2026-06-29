package com.example.imagequality;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestGalleryActivity extends AppCompatActivity {
    private static final int REQUEST_ADD_PHOTOS = 2001;

    // 暴露静态变量供 CompareReportActivity 临时读取对比数据
    public static List<TestSample> currentSamplesRef;

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private AppCompatButton btnAddPhotos;
    private AppCompatButton btnCompareReport;
    private TestGalleryAdapter adapter;
    private List<TestSample> samples;
    
    private QualityAnalyzer analyzer;
    private ImageLoader imageLoader;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_gallery);

        recyclerView = findViewById(R.id.recyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        btnAddPhotos = findViewById(R.id.btnAddPhotos);
        btnCompareReport = findViewById(R.id.btnCompareReport);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        analyzer = new QualityAnalyzer();
        imageLoader = new ImageLoader();
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        
        samples = new ArrayList<>();
        currentSamplesRef = samples;
        adapter = new TestGalleryAdapter(samples);
        recyclerView.setAdapter(adapter);
        
        updateEmptyState();

        btnAddPhotos.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQUEST_ADD_PHOTOS);
        });

        btnCompareReport.setOnClickListener(v -> {
            startActivity(new Intent(this, CompareReportActivity.class));
        });
    }

    private void updateEmptyState() {
        if (samples.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            btnCompareReport.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            btnCompareReport.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ADD_PHOTOS || resultCode != RESULT_OK || data == null) {
            return;
        }

        List<Uri> newUris = new ArrayList<>();
        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                newUris.add(clipData.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            newUris.add(data.getData());
        }

        for (Uri uri : newUris) {
            final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (SecurityException ignored) {
            }
            
            String fileName = getFileName(uri);
            TestSample newSample = new TestSample(uri, fileName);
            samples.add(newSample);
            int newIndex = samples.size() - 1;
            adapter.notifyItemInserted(newIndex);
            
            startAnalysisForSample(newIndex);
        }
        
        updateEmptyState();
    }
    
    private void startAnalysisForSample(final int index) {
        executorService.execute(() -> {
            TestSample sample = samples.get(index);
            try {
                ImageLoader.LoadedImage loadedImage = imageLoader.load(this, sample.getUri());
                sample.setBitmap(loadedImage.getBitmap());
                
                QualityResult result = analyzer.analyze(loadedImage.getBitmap());
                sample.setActualScore(result.getOverallScore());
                sample.setResult(result);
                
                mainHandler.post(() -> adapter.notifyItemChanged(index));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        result = cursor.getString(idx);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "未知图片";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}

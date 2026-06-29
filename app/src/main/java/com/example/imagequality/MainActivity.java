package com.example.imagequality;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_OPEN_IMAGE = 1001;

    // UI 控件绑定
    private View selectPlaceholder;
    private ImageView previewImage;
    private AppCompatButton selectButton;
    private TextView statusText;
    
    private View resultPanel;
    private CircularScoreView scoreView;
    private TextView fileInfoText;
    
    private View summaryCard;
    private TextView summaryText;
    
    private TextView detailsTitleText;
    private LinearLayout metricsContainer;

    // 新增的双 Tab 相关控件
    private View tabLayout;
    private TextView tabOverview;
    private TextView tabDetails;
    private View overviewLayout;
    private View detailsLayout;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ImageLoader imageLoader = new ImageLoader();
    private final QualityAnalyzer analyzer = new QualityAnalyzer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化控件
        selectPlaceholder = findViewById(R.id.selectPlaceholder);
        previewImage = findViewById(R.id.previewImage);
        selectButton = findViewById(R.id.selectButton);
        statusText = findViewById(R.id.statusText);
        
        resultPanel = findViewById(R.id.resultPanel);
        scoreView = findViewById(R.id.scoreView);
        fileInfoText = findViewById(R.id.fileInfoText);
        
        summaryCard = findViewById(R.id.summaryCard);
        summaryText = findViewById(R.id.summaryText);
        
        detailsTitleText = findViewById(R.id.detailsTitleText);
        metricsContainer = findViewById(R.id.metricsContainer);

        // 初始化双 Tab 控件
        tabLayout = findViewById(R.id.tabLayout);
        tabOverview = findViewById(R.id.tabOverview);
        tabDetails = findViewById(R.id.tabDetails);
        overviewLayout = findViewById(R.id.overviewLayout);
        detailsLayout = findViewById(R.id.detailsLayout);

        // 2. 绑定事件监听
        selectPlaceholder.setOnClickListener(v -> openImagePicker());
        selectButton.setOnClickListener(v -> openImagePicker());
        
        // 绑定测试图集按钮
        View btnTestGallery = findViewById(R.id.btnTestGallery);
        if (btnTestGallery != null) {
            btnTestGallery.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, TestGalleryActivity.class));
            });
        }

        // 绑定 Tab 点击事件
        tabOverview.setOnClickListener(v -> switchTab(true));
        tabDetails.setOnClickListener(v -> switchTab(false));

        // 3. 底部小贴士折叠逻辑
        View tipsHeader = findViewById(R.id.tipsHeader);
        View tipsContent = findViewById(R.id.tipsContent);
        TextView tipsToggleArrow = findViewById(R.id.tipsToggleArrow);
        tipsHeader.setOnClickListener(v -> {
            if (tipsContent.getVisibility() == View.GONE) {
                tipsContent.setVisibility(View.VISIBLE);
                tipsToggleArrow.setText("收起");
            } else {
                tipsContent.setVisibility(View.GONE);
                tipsToggleArrow.setText("展开");
            }
        });

        // 4. 处理外部传入的 Intent（用于从测试图集直接跳转查看明细）
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            analyzeImage(intent.getData());
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/jpeg",
                "image/png",
                "image/webp",
                "image/heic",
                "image/heif",
                "image/avif",
                "image/bmp",
                "image/gif"
        });
        startActivityForResult(intent, REQUEST_OPEN_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_IMAGE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        final int flags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // 某些文件提供方不支持持久授权，不影响本次立即分析。
        }
        analyzeImage(uri);
    }

    private void analyzeImage(Uri uri) {
        setLoadingState();
        executor.execute(() -> {
            try {
                ImageLoader.LoadedImage loadedImage = imageLoader.load(this, uri);
                QualityResult result = analyzer.analyze(loadedImage.getBitmap());
                mainHandler.post(() -> renderResult(loadedImage, result));
            } catch (IOException | RuntimeException ex) {
                mainHandler.post(() -> renderError(ex));
            }
        });
    }

    private void setLoadingState() {
        selectPlaceholder.setVisibility(View.GONE);
        previewImage.setVisibility(View.VISIBLE);
        selectButton.setVisibility(View.VISIBLE);
        selectButton.setEnabled(false);
        
        statusText.setText("正在分析图片，请稍候...");

        // 隐藏 Tab 栏与两个页面容器
        tabLayout.setVisibility(View.GONE);
        overviewLayout.setVisibility(View.GONE);
        detailsLayout.setVisibility(View.GONE);
        
        resultPanel.setVisibility(View.VISIBLE);
        scoreView.setScore(0);
        fileInfoText.setText("规格读取中...");
        
        summaryCard.setVisibility(View.GONE);
        detailsTitleText.setVisibility(View.GONE);
        metricsContainer.removeAllViews();
    }

    private void renderResult(ImageLoader.LoadedImage loadedImage, QualityResult result) {
        selectButton.setEnabled(true);
        previewImage.setImageBitmap(loadedImage.getBitmap());
        statusText.setText(String.format(Locale.CHINA,
                "分析完成，耗时 %d ms。", result.getElapsedMs()));
        
        fileInfoText.setText(loadedImage.getMeta().describe());
        scoreView.setScore(result.getOverallScore());
        
        summaryCard.setVisibility(View.VISIBLE);
        summaryText.setText(result.getSummary());
        
        detailsTitleText.setVisibility(View.VISIBLE);
        metricsContainer.removeAllViews();
        for (MetricResult metric : result.getMetrics()) {
            metricsContainer.addView(createMetricCard(metric));
        }

        // 显示 Tab 并默认切换至“基本结果”页
        tabLayout.setVisibility(View.VISIBLE);
        switchTab(true);
    }

    private void renderError(Exception ex) {
        selectButton.setEnabled(true);
        statusText.setText("分析失败：" + ex.getMessage());
        fileInfoText.setText("解析失败，请检查文件格式。");
        scoreView.setScore(0);

        tabLayout.setVisibility(View.GONE);
        overviewLayout.setVisibility(View.GONE);
        detailsLayout.setVisibility(View.GONE);
        
        summaryCard.setVisibility(View.GONE);
        detailsTitleText.setVisibility(View.GONE);
        metricsContainer.removeAllViews();
    }

    private void switchTab(boolean showOverview) {
        if (showOverview) {
            tabOverview.setBackgroundResource(R.drawable.tab_item_selected_bg);
            tabOverview.setTextColor(Color.parseColor("#FFFFFF"));
            tabOverview.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabDetails.setBackgroundColor(Color.TRANSPARENT);
            tabDetails.setTextColor(Color.parseColor("#8A94A6"));
            tabDetails.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            overviewLayout.setVisibility(View.VISIBLE);
            detailsLayout.setVisibility(View.GONE);
        } else {
            tabDetails.setBackgroundResource(R.drawable.tab_item_selected_bg);
            tabDetails.setTextColor(Color.parseColor("#FFFFFF"));
            tabDetails.setTypeface(null, android.graphics.Typeface.BOLD);
            
            tabOverview.setBackgroundColor(Color.TRANSPARENT);
            tabOverview.setTextColor(Color.parseColor("#8A94A6"));
            tabOverview.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            overviewLayout.setVisibility(View.GONE);
            detailsLayout.setVisibility(View.VISIBLE);
        }
    }

    private View createMetricCard(MetricResult metric) {
        View cardView = getLayoutInflater().inflate(R.layout.item_metric, metricsContainer, false);
        
        TextView nameText = cardView.findViewById(R.id.metricName);
        TextView scoreText = cardView.findViewById(R.id.metricScore);
        ProgressBar progress = cardView.findViewById(R.id.metricProgress);
        TextView rawText = cardView.findViewById(R.id.metricRawValue);
        TextView expText = cardView.findViewById(R.id.metricExplanation);

        nameText.setText(metric.getName());
        scoreText.setText(String.format(Locale.CHINA, "%d分", metric.getScore()));
        progress.setProgress(metric.getScore());
        rawText.setText(String.format(Locale.CHINA, "原始指标：%s", metric.getRawValue()));
        expText.setText(metric.getExplanation());

        // 根据分数级别自适应设置字色
        int score = metric.getScore();
        if (score >= 80) {
            scoreText.setTextColor(Color.parseColor("#00F2FE")); // score_excellent 青蓝
        } else if (score >= 70) {
            scoreText.setTextColor(Color.parseColor("#3B82F6")); // score_good 蓝
        } else if (score >= 60) {
            scoreText.setTextColor(Color.parseColor("#FFA048")); // score_fair 黄
        } else {
            scoreText.setTextColor(Color.parseColor("#FF5C5C")); // score_poor 红
        }

        return cardView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}


package com.example.imagequality;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class CompareReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_report);

        ImageView btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> finish());

        List<TestSample> samples = TestGalleryActivity.currentSamplesRef;
        TableLayout tableLayout = findViewById(R.id.compareTableLayout);
        View emptyReportText = findViewById(R.id.emptyReportText);
        View counterExampleCard = findViewById(R.id.counterExampleCard);

        if (samples == null || samples.isEmpty()) {
            tableLayout.setVisibility(View.GONE);
            counterExampleCard.setVisibility(View.GONE);
            emptyReportText.setVisibility(View.VISIBLE);
            return;
        }

        emptyReportText.setVisibility(View.GONE);
        tableLayout.setVisibility(View.VISIBLE);
        
        // 智能侦测景深虚化反例：
        // 算法逻辑：若清晰度极低 (<=60)，但曝光或噪点等其他物理指标优秀 (>=75)，则大概率为虚化被误判的好图
        boolean hasCounterExample = false;
        for (TestSample sample : samples) {
            QualityResult res = sample.getResult();
            if (res != null) {
                int sharp = getMetricScore(res, "清晰度");
                int exp = getMetricScore(res, "曝光");
                int noise = getMetricScore(res, "噪点");
                
                if (sharp != -1 && sharp <= 60 && (exp >= 75 || noise >= 75)) {
                    hasCounterExample = true;
                    break;
                }
            }
        }
        counterExampleCard.setVisibility(hasCounterExample ? View.VISIBLE : View.GONE);

        buildTable(tableLayout, samples);
    }

    private void buildTable(TableLayout tableLayout, List<TestSample> samples) {
        tableLayout.removeAllViews();
        int padding = dpToPx(8);

        // Header Row (Images Names)
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#1E2025"));
        headerRow.addView(createCell("指标名称", true, padding));
        for (TestSample sample : samples) {
            String name = sample.getFileName();
            if (name.length() > 10) {
                name = name.substring(0, 8) + "...";
            }
            headerRow.addView(createCell(name, true, padding));
        }
        tableLayout.addView(headerRow);

        // Overall Score Row
        TableRow scoreRow = new TableRow(this);
        scoreRow.setBackgroundColor(Color.parseColor("#2A2D35"));
        scoreRow.addView(createCell("综合评分", true, padding));
        for (TestSample sample : samples) {
            String scoreStr = sample.getActualScore() == -1 ? "计算中" : sample.getActualScore() + " 分";
            TextView tv = createCell(scoreStr, true, padding);
            if (sample.getActualScore() >= 80) tv.setTextColor(Color.parseColor("#00F2FE"));
            else if (sample.getActualScore() >= 60) tv.setTextColor(Color.parseColor("#FFA048"));
            else tv.setTextColor(Color.parseColor("#FF5C5C"));
            scoreRow.addView(tv);
        }
        tableLayout.addView(scoreRow);

        // User Note Row (Optional but useful for presentation)
        TableRow noteRow = new TableRow(this);
        noteRow.setBackgroundColor(Color.parseColor("#1E2025"));
        noteRow.addView(createCell("人工/备注", true, padding));
        boolean hasNotes = false;
        for (TestSample sample : samples) {
            String note = sample.getUserNote();
            if (!TextUtils.isEmpty(note)) hasNotes = true;
            noteRow.addView(createCell(TextUtils.isEmpty(note) ? "-" : note, false, padding));
        }
        if (hasNotes) {
            tableLayout.addView(noteRow);
        }

        // Detailed Metrics
        // Find a sample that has result to get the metric names
        QualityResult sampleResult = null;
        for (TestSample sample : samples) {
            if (sample.getResult() != null && !sample.getResult().getMetrics().isEmpty()) {
                sampleResult = sample.getResult();
                break;
            }
        }

        if (sampleResult != null) {
            List<MetricResult> referenceMetrics = sampleResult.getMetrics();
            boolean isDarkRow = false;
            for (int i = 0; i < referenceMetrics.size(); i++) {
                TableRow metricRow = new TableRow(this);
                metricRow.setBackgroundColor(isDarkRow ? Color.parseColor("#1E2025") : Color.parseColor("#2A2D35"));
                isDarkRow = !isDarkRow;
                
                String metricName = referenceMetrics.get(i).getName();
                metricRow.addView(createCell(metricName, false, padding));
                
                for (TestSample sample : samples) {
                    QualityResult res = sample.getResult();
                    String cellValue = "-";
                    if (res != null && i < res.getMetrics().size()) {
                        cellValue = res.getMetrics().get(i).getScore() + " 分";
                    }
                    metricRow.addView(createCell(cellValue, false, padding));
                }
                tableLayout.addView(metricRow);
            }
        }
    }

    private TextView createCell(String text, boolean isHeader, int padding) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(padding, padding, padding, padding);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(isHeader ? Color.parseColor("#FFFFFF") : Color.parseColor("#8A94A6"));
        tv.setTextSize(isHeader ? 13f : 12f);
        if (isHeader) {
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        tv.setBackgroundResource(R.drawable.card_background); // Optional, for border
        
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(1, 1, 1, 1);
        tv.setLayoutParams(params);
        return tv;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getMetricScore(QualityResult res, String namePrefix) {
        if (res == null || res.getMetrics() == null) return -1;
        for (MetricResult m : res.getMetrics()) {
            if (m.getName().startsWith(namePrefix)) {
                return m.getScore();
            }
        }
        return -1;
    }
}

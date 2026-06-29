package com.example.imagequality;

import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TestGalleryAdapter extends RecyclerView.Adapter<TestGalleryAdapter.ViewHolder> {

    private List<TestSample> samples;

    public TestGalleryAdapter(List<TestSample> samples) {
        this.samples = samples;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_sample, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestSample sample = samples.get(position);
        
        // Remove existing text watcher to avoid triggering during recycle
        if (holder.textWatcher != null) {
            holder.noteEditText.removeTextChangedListener(holder.textWatcher);
        }

        holder.fileNameText.setText(sample.getFileName());
        holder.noteEditText.setText(sample.getUserNote());
        
        if (sample.getBitmap() != null) {
            holder.sampleImage.setImageBitmap(sample.getBitmap());
        } else {
            holder.sampleImage.setImageBitmap(null);
        }

        if (sample.getActualScore() != -1) {
            int score = sample.getActualScore();
            holder.actualScoreText.setText(score + " 分");
            
            if (score >= 80) {
                holder.actualScoreText.setTextColor(Color.parseColor("#00F2FE"));
            } else if (score >= 70) {
                holder.actualScoreText.setTextColor(Color.parseColor("#3B82F6"));
            } else if (score >= 60) {
                holder.actualScoreText.setTextColor(Color.parseColor("#FFA048"));
            } else {
                holder.actualScoreText.setTextColor(Color.parseColor("#FF5C5C"));
            }
        } else {
            holder.actualScoreText.setText("等待分析...");
            holder.actualScoreText.setTextColor(Color.parseColor("#8A94A6"));
            holder.clickHintText.setVisibility(View.GONE);
        }
        
        if (sample.getResult() != null) {
            holder.clickHintText.setVisibility(View.VISIBLE);
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                intent.setData(sample.getUri());
                v.getContext().startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }

        // Save user input
        holder.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                sample.setUserNote(s.toString());
            }
        };
        holder.noteEditText.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return samples == null ? 0 : samples.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView sampleImage;
        TextView fileNameText;
        TextView actualScoreText;
        EditText noteEditText;
        TextView clickHintText;
        TextWatcher textWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            sampleImage = itemView.findViewById(R.id.sampleImage);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            actualScoreText = itemView.findViewById(R.id.actualScoreText);
            noteEditText = itemView.findViewById(R.id.noteEditText);
            clickHintText = itemView.findViewById(R.id.clickHintText);
        }
    }
}

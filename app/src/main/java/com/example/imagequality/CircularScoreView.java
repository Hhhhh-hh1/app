package com.example.imagequality;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class CircularScoreView extends View {
    private Paint bgPaint;
    private Paint progressPaint;
    private Paint textScorePaint;
    private Paint textLabelPaint;
    
    private RectF ovalRect;
    private int score = 0;
    private float animatedScore = 0f;
    private ValueAnimator animator;
    
    private int strokeWidth = 24; // 默认圆环宽度，会在 init 中根据 dp 调整
    
    public CircularScoreView(Context context) {
        super(context);
        init(context, null);
    }
    
    public CircularScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public CircularScoreView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        float density = context.getResources().getDisplayMetrics().density;
        strokeWidth = (int) (10 * density + 0.5f); // 10dp
        
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(strokeWidth);
        bgPaint.setColor(Color.parseColor("#1F2438")); // 暗底环
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        textScorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textScorePaint.setTextAlign(Paint.Align.CENTER);
        textScorePaint.setFakeBoldText(true);
        
        textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textLabelPaint.setTextAlign(Paint.Align.CENTER);
        textLabelPaint.setColor(Color.parseColor("#8A94A6")); // 科技灰
        textLabelPaint.setTextSize(12 * density);
        
        ovalRect = new RectF();
    }
    
    public void setScore(int newScore) {
        this.score = Math.max(0, Math.min(100, newScore));
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        
        animator = ValueAnimator.ofFloat(0f, (float) this.score);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedScore = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getContext().getResources().getDisplayMetrics().density;
        
        // 留出圆环宽度的 padding，避免弧线被裁剪
        float pad = strokeWidth / 2f + 4 * density;
        float size = Math.min(w, h);
        ovalRect.set(pad, pad, size - pad, size - pad);
        
        // 设置渐变色背景前景环
        // 从左上到右下
        LinearGradient gradient = new LinearGradient(
            ovalRect.left, ovalRect.top, ovalRect.right, ovalRect.bottom,
            Color.parseColor("#6C5DD3"), // grad_start
            Color.parseColor("#A076F9"), // grad_end
            Shader.TileMode.CLAMP
        );
        progressPaint.setShader(gradient);
        
        textScorePaint.setTextSize(size * 0.28f); // 动态匹配大小
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 1. 绘制背景圆环
        canvas.drawArc(ovalRect, 0, 360, false, bgPaint);
        
        // 2. 绘制进度圆环（从正上方 -90度 开始）
        float sweepAngle = (animatedScore / 100f) * 360f;
        canvas.drawArc(ovalRect, -90, sweepAngle, false, progressPaint);
        
        // 3. 绘制文字分数
        int displayScore = Math.round(animatedScore);
        
        // 分数颜色根据分数划分
        if (displayScore >= 80) {
            textScorePaint.setColor(Color.parseColor("#00F2FE")); // score_excellent 青蓝
        } else if (displayScore >= 70) {
            textScorePaint.setColor(Color.parseColor("#3B82F6")); // score_good 蓝
        } else if (displayScore >= 60) {
            textScorePaint.setColor(Color.parseColor("#FFA048")); // score_fair 黄
        } else {
            textScorePaint.setColor(Color.parseColor("#FF5C5C")); // score_poor 红
        }
        
        // 计算文本垂直中心
        float scoreY = ovalRect.centerY() + (textScorePaint.getTextSize() / 3f) - (strokeWidth / 4f);
        canvas.drawText(String.valueOf(displayScore), ovalRect.centerX(), scoreY, textScorePaint);
        
        // 绘制“综合评分”小标签
        float labelY = scoreY + textLabelPaint.getTextSize() + 8 * getContext().getResources().getDisplayMetrics().density;
        canvas.drawText("综合评分", ovalRect.centerX(), labelY, textLabelPaint);
    }
}

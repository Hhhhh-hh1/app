package com.example.imagequality;

import android.graphics.Bitmap;
import android.net.Uri;

public class TestSample {
    private Uri uri;
    private String fileName;
    private String userNote;
    private int actualScore = -1;
    private Bitmap bitmap;

    public TestSample(Uri uri, String fileName) {
        this.uri = uri;
        this.fileName = fileName;
        this.userNote = "";
    }

    public Uri getUri() { return uri; }
    public String getFileName() { return fileName; }
    
    public String getUserNote() { return userNote; }
    public void setUserNote(String userNote) { this.userNote = userNote; }
    
    public int getActualScore() { return actualScore; }
    public void setActualScore(int score) { this.actualScore = score; }

    public Bitmap getBitmap() { return bitmap; }
    public void setBitmap(Bitmap bitmap) { this.bitmap = bitmap; }

    private QualityResult result;
    public QualityResult getResult() { return result; }
    public void setResult(QualityResult result) { this.result = result; }

    private boolean isExpanded = false;
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}

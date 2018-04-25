package com.worksmobile.wmproject.value_object;

public class DownloadItem {
    private String fileName;
    private String downlodDate;
    private String imageLink;
    private int progress;
    private int width;
    private int height;
    private boolean isFinished;

    public DownloadItem(String fileName, String downlodDate, String imageLink, int progress, int width, int height) {
        this.fileName = fileName;
        this.downlodDate = downlodDate;
        this.imageLink = imageLink;
        this.progress = progress;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownlodDate() {
        return downlodDate;
    }

    public void setDownlodDate(String downlodDate) {
        this.downlodDate = downlodDate;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

}

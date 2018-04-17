package com.worksmobile.wmproject.retrofit_object;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MediaMetadata implements Serializable {
    @SerializedName("width")
    private int width;
    @SerializedName("height")
    private int height;
    @SerializedName("time")
    private String time;

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

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "width : " + width + ", height : " + height + ", time : " + time;
    }
}

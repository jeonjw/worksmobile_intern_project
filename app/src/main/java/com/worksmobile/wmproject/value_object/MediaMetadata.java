package com.worksmobile.wmproject.value_object;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class MediaMetadata implements Serializable {
    @SerializedName("width")
    private int width;
    @SerializedName("height")
    private int height;
    @SerializedName("time")
    private String time;
    @SerializedName("isoSpeed")
    private int isoSpeed;
    @SerializedName("cameraModel")
    private String cameraModel;
    @SerializedName("meteringMode")
    private String meteringMode;
    @SerializedName("exposureMode")
    private String exposureMode;
    @SerializedName("focalLength")
    private float focalLength;
    @SerializedName("location")
    private LocationInfo locationInfo;


    public String getCameraModel() {
        return cameraModel;
    }

    public String getMeteringMode() {
        return meteringMode;
    }

    public String getExposureMode() {
        return exposureMode;
    }

    public float getFocalLength() {
        return focalLength;
    }

    public int getIsoSpeed() {
        return isoSpeed;
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

    public String getTime() {
        return time;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(LocationInfo locationInfo) {
        this.locationInfo = locationInfo;
    }

    @Override
    public String toString() {
        return "width : " + width + ", height : " + height + ", time : " + time + ", locationInfo : " + locationInfo;
    }
}

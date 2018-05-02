package com.worksmobile.wmproject.value_object;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Properties implements Serializable {
    @SerializedName("latitude1")
    private double latitude1;
    @SerializedName("longitude1")
    private double longitude1;


    @SerializedName("latitude2")
    private double latitude2;
    @SerializedName("longitude2")
    private double longitude2;


    @SerializedName("latitude3")
    private double latitude3;
    @SerializedName("longitude3")
    private double longitude3;

    @SerializedName("latitude4")
    private double latitude4;
    @SerializedName("longitude4")
    private double longitude4;


    @SerializedName("latitude5")
    private double latitude5;
    @SerializedName("longitude5")
    private double longitude5;


    @SerializedName("latitude6")
    private double latitude6;
    @SerializedName("longitude6")
    private double longitude6;


    @SerializedName("latitude7")
    private double latitude7;
    @SerializedName("longitude7")
    private double longitude7;

    @SerializedName("latitude8")
    private double latitude8;
    @SerializedName("longitude8")
    private double longitude8;


    public double getLatitude8() {
        return latitude7;
    }

    public double getLongitude8() {
        return longitude7;
    }

}

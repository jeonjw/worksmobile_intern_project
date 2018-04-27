package com.worksmobile.wmproject.value_object;

import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MarkerItem implements ClusterItem {

    private String imageUrl;
    private LatLng latLng;
    private Drawable drawable;
    private int resouceId;

    public MarkerItem(double latitude, double longitude, int resourceId) {
        latLng = new LatLng(latitude, longitude);
        this.resouceId = resourceId;
    }

    public int getResouceId() {
        return resouceId;
    }

    public void setResouceId(int resouceId) {
        this.resouceId = resouceId;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public MarkerItem(double latitude, double longitude, String imageUrl) {

        latLng = new LatLng(latitude, longitude);
        this.imageUrl = imageUrl;
    }

    public double getLatitude() {
        return latLng.latitude;
    }

    public double getLongitude() {
        return latLng.longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }
}

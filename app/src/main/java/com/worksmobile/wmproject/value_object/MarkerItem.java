package com.worksmobile.wmproject.value_object;

import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MarkerItem implements ClusterItem {

    private String imageUrl;
    private LatLng latLng;
    private Drawable drawable;
    private String addressName;

    public MarkerItem(double latitude, double longitude, String imageUrl) {

        latLng = new LatLng(latitude, longitude);
        this.imageUrl = imageUrl;
    }

    public MarkerItem(LatLng latLng, String addressName, Drawable resouceId) {
        this.latLng = latLng;
        this.addressName = addressName;
        this.drawable = resouceId;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public String getAddressName() {
        return addressName;
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

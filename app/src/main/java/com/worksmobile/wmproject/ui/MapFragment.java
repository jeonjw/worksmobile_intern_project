package com.worksmobile.wmproject.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.GlideApp;
import com.worksmobile.wmproject.MultiDrawable;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.value_object.DriveFile;
import com.worksmobile.wmproject.value_object.MarkerItem;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener,
        ClusterManager.OnClusterClickListener<MarkerItem>,
        ClusterManager.OnClusterInfoWindowClickListener<MarkerItem>,
        ClusterManager.OnClusterItemClickListener<MarkerItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem> {

    private static final String TAG = "MAP_FRAGMENT";

    private MapView mapView;
    private GoogleMap googleMap;
    private boolean locationPermmisionGranted;
    private Set<String> photoHistory;
    private Set<String> queryHistory;
    private Set<DriveFile> queryFiles;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private ClusterManager<MarkerItem> clusterManager;
    private String queryLatDegree;
    private String queryLngDegree;

    private DriveHelper driveHelper;
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.toolbar_sorting_button).setVisible(false);
        menu.findItem(R.id.toolbar_check_button).setVisible(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map_view);
        mapView.getMapAsync(this);

        photoHistory = new HashSet<>();
        queryHistory = new HashSet<>();
        queryFiles = new HashSet<>();

        driveHelper = new DriveHelper(getActivity());
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "OnMapReady");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermmisionGranted = true;
        } else {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, 101);
        }

        this.googleMap = googleMap;
        clusterManager = new ClusterManager<>(getActivity(), this.googleMap);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.35944, 127.10527)));
        this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));

        clusterManager = new ClusterManager<>(getActivity(), this.googleMap);
        clusterManager.setRenderer(new PersonRenderer());
        this.googleMap.setOnCameraIdleListener(this);
        this.googleMap.setOnCameraMoveListener(this);
        this.googleMap.setOnMarkerClickListener(clusterManager);
        this.googleMap.setOnInfoWindowClickListener(clusterManager);
        clusterManager.setOnClusterClickListener(this);
        clusterManager.setOnClusterInfoWindowClickListener(this);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterItemInfoWindowClickListener(this);

        updateLocationUI();
    }

    @Override
    public void onCameraIdle() {
        clusterManager.onCameraIdle();
        LatLng latLng = googleMap.getCameraPosition().target;
        LatLngBounds latLngBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        System.out.println("CAMERA CENTER POSTITION : " + latLng.latitude + ", " + latLng.longitude);
        DecimalFormat decimalFormat = new DecimalFormat("000.00000");

        System.out.println("CAMERA MAX BOUNDS N/E Lati: " + decimalFormat.format(latLngBounds.northeast.latitude));
        System.out.println("CAMERA MAX BOUNDS N/E Longi: " + decimalFormat.format(latLngBounds.northeast.longitude));
        System.out.println("CAMERA MAX BOUNDS S/W Lati: " + decimalFormat.format(latLngBounds.southwest.latitude));
        System.out.println("CAMERA MAX BOUNDS S/W Longi: " + decimalFormat.format(latLngBounds.southwest.longitude));
        System.out.println("CAMERA ZOOM LEVEL : " + googleMap.getCameraPosition().zoom);

        String latQuery = matchGeoRange("latitude", decimalFormat.format(latLngBounds.northeast.latitude), decimalFormat.format(latLngBounds.southwest.latitude));
        String lngQuery = matchGeoRange("longitude", decimalFormat.format(latLngBounds.northeast.longitude), decimalFormat.format(latLngBounds.southwest.longitude));

        System.out.println("lat Query : " + latQuery);
        System.out.println("lng Query : " + lngQuery);

        requestPhotoIdAndProperties(latQuery, lngQuery);
    }

    @Override
    public void onCameraMove() {
        fetchPhoto();
    }

    private String matchGeoRange(String key, String degree1, String degree2) {
        StringBuilder builder = new StringBuilder();
        boolean finish = false;
        int level = 0;
        for (int i = 0; i < degree1.length(); i++) {
            if (!finish && degree1.charAt(i) == degree2.charAt(i)) {
                builder.append(degree1.charAt(i));
                if (degree1.charAt(i) != '.') {
                    level++;
                }

            } else {
                finish = true;
                if (degree1.charAt(i) == '.') {
                    builder.append(".");
                } else {
                    builder.append("0");
                }
            }
        }

        String newDegree = builder.toString();
        if (newDegree.length() > 0 && newDegree.charAt(newDegree.length() - 1) == '.') {
            newDegree = newDegree.substring(0, newDegree.length() - 1);
        }

        @SuppressLint("DefaultLocale")
        String query = String.format("properties has { key='%s%d' and value = '%s' }", key, level, newDegree);

        if (key.equals("latitude")) {
            queryLatDegree = newDegree;
        } else if (key.equals("longitude")) {
            queryLngDegree = newDegree;
        }
        return query;

    }

    private void requestPhotoIdAndProperties(String latQuery, String lngQuery) {
        if (queryHistory.contains(queryLatDegree) && queryHistory.contains(queryLngDegree)) {
            System.out.println("QUERY 진행 패스");
            fetchPhoto();
            return;
        }

        driveHelper.getPhotoIdAndProperties(latQuery, lngQuery, new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {

                queryHistory.add(queryLatDegree);
                queryHistory.add(queryLngDegree);
                System.out.println("BEFROE SIZE : " + driveFiles.length);


                Collections.addAll(queryFiles, driveFiles);

                System.out.println("ADDED SIZE  : " + queryFiles.size());

                fetchPhoto();
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void fetchPhoto() {
        if (queryFiles.size() == photoHistory.size()) {
            return;
        }

        double limit = SphericalUtil.computeDistanceBetween(googleMap.getCameraPosition().target, googleMap.getProjection().getVisibleRegion().farRight);
        for (DriveFile file : queryFiles) {
            LatLng latLng = new LatLng(file.getProperties().getLatitude8(), file.getProperties().getLongitude8());
            if (SphericalUtil.computeDistanceBetween(googleMap.getCameraPosition().target, latLng) <= limit && !photoHistory.contains(file.getName())) {
                System.out.println("추가됨 : " + file.getName());
                photoHistory.add(file.getName());
                addMarker(new LatLng(file.getProperties().getLatitude8(), file.getProperties().getLongitude8()), file.getThumbnailLink());
            }
        }
    }

    private void addMarker(LatLng latLng, String imageUrl) {
        GlideApp.with(getActivity())
                .load(imageUrl)
                .override(200, 200)
                .fitCenter()
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        clusterManager.addItem(new MarkerItem(latLng, getAddressNameFromLatLng(latLng), resource));
                        clusterManager.cluster();
                    }
                });
    }

    private String getAddressNameFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String addressName = "";
        if (addresses != null) {
            if (addresses.size() == 0) {
                addressName = "주소 정보 없음";
            } else {
                if (addresses.get(0).getLocality() != null)
                    addressName += addresses.get(0).getLocality();
                if (addresses.get(0).getThoroughfare() != null) {
                    addressName += " " + addresses.get(0).getThoroughfare();
                }
            }
        }

        return addressName;
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (locationPermmisionGranted) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            googleMap.setMyLocationEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        googleMap.getUiSettings().setRotateGesturesEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (permissions.length == 1 && permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationPermmisionGranted = true;
                }
            } else {
                Toast.makeText(getActivity(), "지도 권한 없음", Toast.LENGTH_SHORT).show();
            }
        }
        updateLocationUI();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        mapView.onCreate(savedInstanceState);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public boolean onClusterClick(Cluster<MarkerItem> cluster) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        final LatLngBounds bounds = builder.build();

        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private class PersonRenderer extends DefaultClusterRenderer<MarkerItem> {
        private final IconGenerator iconGenerator = new IconGenerator(getActivity());
        private final IconGenerator clusterIconGenerator = new IconGenerator(getActivity());
        private final ImageView imageView;
        private final ImageView clusterImageView;
        private final int dimension;

        public PersonRenderer() {
            super(getActivity(), googleMap, clusterManager);

            View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile, null);
            clusterIconGenerator.setContentView(multiProfile);
            clusterImageView = multiProfile.findViewById(R.id.multi_profile_imageview);

            imageView = new ImageView(getActivity());
            dimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(dimension, dimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            imageView.setPadding(padding, padding, padding, padding);
            iconGenerator.setContentView(imageView);
        }

        @Override
        protected void onBeforeClusterItemRendered(MarkerItem markerItem, MarkerOptions markerOptions) {
            imageView.setImageDrawable(markerItem.getDrawable());
            Bitmap icon = iconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(markerItem.getAddressName());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<MarkerItem> cluster, MarkerOptions markerOptions) {
            List<Drawable> profilePhotos = new ArrayList<>(Math.min(4, cluster.getSize()));
            int width = dimension;
            int height = dimension;

            for (MarkerItem p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                Drawable drawable = p.getDrawable();
                drawable.setBounds(0, 0, width, height);
                profilePhotos.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);

            clusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = clusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return cluster.getSize() > 1;
        }
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<MarkerItem> cluster) {
    }

    @Override
    public boolean onClusterItemClick(MarkerItem markerItem) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(MarkerItem markerItem) {

    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

}

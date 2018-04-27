package com.worksmobile.wmproject.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener,
        ClusterManager.OnClusterClickListener<MarkerItem>,
        ClusterManager.OnClusterInfoWindowClickListener<MarkerItem>,
        ClusterManager.OnClusterItemClickListener<MarkerItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<MarkerItem> {

    private static final String TAG = "MAP_FRAGMENT";

    private MapView mapView;
    private GoogleMap googleMap;
    private boolean locationPermmisionGranted;
    private Set<String> history;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};
    private ClusterManager<MarkerItem> clusterManager;

    @Override
    public void onCameraIdle() {
        clusterManager.onCameraIdle();
        Toast.makeText(getActivity(), "Camera movement Idle.", Toast.LENGTH_SHORT).show();
        LatLng latLng = googleMap.getCameraPosition().target;
        LatLngBounds latLngBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        System.out.println("CAMERA IDLE : " + latLng.latitude + ", " + latLng.longitude);
        DecimalFormat decimalFormat = new DecimalFormat("000.00000");

        System.out.println("CAMERA MAX BOUNDS N/E Lati: " + decimalFormat.format(latLngBounds.northeast.latitude));
        System.out.println("CAMERA MAX BOUNDS N/E Longi: " + decimalFormat.format(latLngBounds.northeast.longitude));
        System.out.println("CAMERA MAX BOUNDS S/W Lati: " + decimalFormat.format(latLngBounds.southwest.latitude));
        System.out.println("CAMERA MAX BOUNDS S/W Longi: " + decimalFormat.format(latLngBounds.southwest.longitude));
        System.out.println("ZOOM LEVEL : " + googleMap.getCameraPosition().zoom);

        String latQuery = matchGeoRange("latitude", decimalFormat.format(latLngBounds.northeast.latitude), decimalFormat.format(latLngBounds.southwest.latitude));
        String lngQuery = matchGeoRange("longitude", decimalFormat.format(latLngBounds.northeast.longitude), decimalFormat.format(latLngBounds.southwest.longitude));

        System.out.println("lat Query : " + latQuery);
        System.out.println("lng Query : " + lngQuery);

        requestPhotoIdAndProperties(latQuery, lngQuery);
    }


    private class PersonRenderer extends DefaultClusterRenderer<MarkerItem> {
        private final IconGenerator mIconGenerator = new IconGenerator(getActivity());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getActivity());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public PersonRenderer() {
            super(getActivity(), googleMap, clusterManager);

            View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile, null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = multiProfile.findViewById(R.id.multi_profile_imageview);

            mImageView = new ImageView(getActivity());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }

        @Override
        protected void onBeforeClusterItemRendered(MarkerItem markerItem, MarkerOptions markerOptions) {
            mImageView.setImageDrawable(markerItem.getDrawable());
            Bitmap icon = mIconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title("test");
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<MarkerItem> cluster, MarkerOptions markerOptions) {
            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = mDimension;
            int height = mDimension;

            for (MarkerItem p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                Drawable drawable = p.getDrawable();
                drawable.setBounds(0, 0, width, height);
                profilePhotos.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);

            mClusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return cluster.getSize() > 1;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        history = new HashSet<>();
        mapView = view.findViewById(R.id.map_view);
        mapView.getMapAsync(this);
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        return view;
    }

    private void requestPhotoIdAndProperties(String latQuery, String lngQuery) {
        DriveHelper driveHelper = new DriveHelper(getActivity());

        driveHelper.getPhotoIdAndProperties(latQuery, lngQuery, new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                double limit = SphericalUtil.computeDistanceBetween(googleMap.getCameraPosition().target, googleMap.getProjection().getVisibleRegion().farRight);
                List<String> fetchList = new ArrayList<>();

                for (DriveFile file : driveFiles) {
                    LatLng latLng = new LatLng(file.getProperties().getLatitude8(), file.getProperties().getLongitude8());
                    if (SphericalUtil.computeDistanceBetween(googleMap.getCameraPosition().target, latLng) <= limit) {
                        if (!history.contains(file.getName()))
                            fetchList.add(file.getName());
                    }
                }

                System.out.println("Fetch Size : " + fetchList.size());
                if (fetchList.size() != 0) {
                    driveHelper.getFileListFromName(fetchList, new ListCallback() {
                        @Override
                        public void onSuccess(DriveFile[] driveFiles) {
                            for (DriveFile file : driveFiles) {
                                history.add(file.getName());
                                addMarker(new LatLng(file.getProperties().getLatitude8(), file.getProperties().getLongitude8()), file.getThumbnailLink());
                            }
                            System.out.println("ADD FINISH");

                        }

                        @Override
                        public void onFailure(String msg) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void addMarker(LatLng latLng, String imageUrl) {
        GlideApp.with(getActivity())
                .load(imageUrl)
                .override(200, 200)
                .fitCenter()
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        clusterManager.addItem(new MarkerItem(latLng, resource));
                        clusterManager.cluster();
                    }
                });
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
        this.googleMap.setOnMarkerClickListener(clusterManager);
        this.googleMap.setOnInfoWindowClickListener(clusterManager);
        clusterManager.setOnClusterClickListener(this);
        clusterManager.setOnClusterInfoWindowClickListener(this);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterItemInfoWindowClickListener(this);


        updateLocationUI();
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
        @SuppressLint("DefaultLocale") String query = String.format("properties has { key='%s%d' and value = '%s' }", key, level, newDegree);
        return query;
    }

    @Override
    public boolean onClusterClick(Cluster<MarkerItem> cluster) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
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

}

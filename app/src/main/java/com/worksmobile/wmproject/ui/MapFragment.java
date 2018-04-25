package com.worksmobile.wmproject.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.GlideApp;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.value_object.DriveFile;
import com.worksmobile.wmproject.value_object.MarkerItem;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveCanceledListener {
    private static final String TAG = "MAP_FRAGMENT";

    private MapView mapView;
    private GoogleMap googleMap;
    private View markerView;
    private ImageView markerImageView;
    private boolean locationPermmisionGranted;
    private LocationManager locationManager;
    private List<MarkerItem> markerList;

    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.map_view);
        mapView.getMapAsync(this);
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }


        setCustomMarkerView();
        requestWholeList();

        return view;
    }

//    private void getSampleMarkerItems() {
//        List<MarkerItem> sampleList = new ArrayList<>();
//        sampleList.add(new MarkerItem(37.538523, 126.96568, "http://www.myiconfinder.com/uploads/iconsets/256-256-a5485b563efc4511e0cd8bd04ad0fe9e.png"));
//        sampleList.add(new MarkerItem(37.527523, 126.96568, "http://www.myiconfinder.com/uploads/iconsets/256-256-a5485b563efc4511e0cd8bd04ad0fe9e.png"));
//        sampleList.add(new MarkerItem(37.549523, 126.96568, "http://www.myiconfinder.com/uploads/iconsets/256-256-a5485b563efc4511e0cd8bd04ad0fe9e.png"));
//        sampleList.add(new MarkerItem(37.538523, 126.95768, "http://www.myiconfinder.com/uploads/iconsets/256-256-a5485b563efc4511e0cd8bd04ad0fe9e.png"));
//
//        for (MarkerItem markerItem : sampleList) {
//            addMarker(markerItem);
//        }
//    }

    private void requestWholeList() {
        DriveHelper driveHelper = new DriveHelper(getActivity());
        markerList = new ArrayList<>();

        driveHelper.enqueuePhotoMapListCreationCall(new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                for (DriveFile file : driveFiles) {
                    addMarker(new MarkerItem(file.getProperties().getLatitude(), file.getProperties().getLongitude(), file.getThumbnailLink()));
                }
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void addMarker(MarkerItem markerItem) {
        LatLng position = new LatLng(markerItem.getLatitude(), markerItem.getLongitude());
        String imageUrl = markerItem.getImageUrl();

        System.out.println("POSITION : " + markerItem.getLatitude() + " " + markerItem.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title("Test");
        markerOptions.position(position);

        GlideApp.with(this)
                .load(imageUrl)
                .override(80, 80)
                .fitCenter()
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        markerImageView.setImageDrawable(resource);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(markerView)));
                        googleMap.addMarker(markerOptions);
                    }
                });
    }

    private Bitmap createDrawableFromView(View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }


    private void setCustomMarkerView() {
        markerView = LayoutInflater.from(getActivity()).inflate(R.layout.marker_item, null);
        markerImageView = markerView.findViewById(R.id.marker_imageview);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "OnMapReady");
        this.googleMap = googleMap;
        this.googleMap.setOnCameraIdleListener(this);
        this.googleMap.setOnCameraMoveStartedListener(this);
        this.googleMap.setOnCameraMoveCanceledListener(this);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.35944, 127.10527)));

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                LatLng latLng = new LatLng(lat, lng);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("내위치");
                markerOptions.snippet("내위치");
                googleMap.addMarker(markerOptions);
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(9));
                Log.d(TAG, "latitude: " + lat + ", longitude: " + lng);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermmisionGranted = true;
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
        } else {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, 101);
        }

        updateLocationUI();
//        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.35944, 127.10527)));
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(7));

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

    @Override
    public void onCameraIdle() {
        Toast.makeText(getActivity(), "Camera movement Idle.",
                Toast.LENGTH_SHORT).show();
        LatLng latLng = googleMap.getCameraPosition().target;
        System.out.println("CAMERA IDLE : " + latLng.latitude + ", " + latLng.longitude);
        System.out.println("CAMERA MAX BOUNDS N/E Lati: " + googleMap.getProjection().getVisibleRegion().latLngBounds.northeast.latitude);
        System.out.println("CAMERA MAX BOUNDS N/E Longi: " + googleMap.getProjection().getVisibleRegion().latLngBounds.northeast.longitude);
        System.out.println("CAMERA MAX BOUNDS S/W Lati: " + googleMap.getProjection().getVisibleRegion().latLngBounds.southwest.latitude);
        System.out.println("CAMERA MAX BOUNDS S/W Longi: " + googleMap.getProjection().getVisibleRegion().latLngBounds.southwest.longitude);

    }

    @Override
    public void onCameraMoveCanceled() {
        Toast.makeText(getActivity(), "Camera movement canceled.",
                Toast.LENGTH_SHORT).show();
        LatLng latLng = googleMap.getCameraPosition().target;
        System.out.println("CAMERA CANCEL : " + latLng.latitude + ", " + latLng.longitude);
    }


    @Override
    public void onCameraMoveStarted(int i) {

    }
}

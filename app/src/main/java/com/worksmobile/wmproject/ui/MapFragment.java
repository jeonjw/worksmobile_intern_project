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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.GlideApp;
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
        GoogleMap.OnCameraMoveStartedListener {
    private static final String TAG = "MAP_FRAGMENT";

    private MapView mapView;
    private GoogleMap googleMap;
    private View markerView;
    private ImageView markerImageView;
    private boolean locationPermmisionGranted;
    private LocationManager locationManager;
    private Set<String> history;
    private static final String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        history = new HashSet<>();
        mapView = view.findViewById(R.id.map_view);
        mapView.getMapAsync(this);
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }


        setCustomMarkerView();
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
                                addMarker(new MarkerItem(file.getProperties().getLatitude8(), file.getProperties().getLongitude8(), file.getThumbnailLink()));
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


    private void requestPhotoListWithDegree(String latQuery, String lngQuery) {
        DriveHelper driveHelper = new DriveHelper(getActivity());

        driveHelper.enqueuePhotoMapListCreationCall(latQuery, lngQuery, new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                for (DriveFile file : driveFiles) {
                    addMarker(new MarkerItem(file.getProperties().getLatitude8(), file.getProperties().getLongitude8(), file.getThumbnailLink()));
                }
                System.out.println("ADD FINISH");
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void addMarker(MarkerItem markerItem) {
        LatLng position = new LatLng(markerItem.getLatitude(), markerItem.getLongitude());
        String imageUrl = markerItem.getImageUrl();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title("Test");
        markerOptions.position(position);

        GlideApp.with(getActivity())
                .load(imageUrl)
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
        this.googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println("마커 클릭 : " + marker.getPosition().latitude + ", " + marker.getPosition().longitude);
                return false;
            }
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(37.35944, 127.10527)));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(9));

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                Log.d(TAG, "내위치 latitude: " + lat + ", longitude: " + lng);
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

//        requestPhotoListWithDegree(null, null);

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

    @SuppressLint("DefaultLocale")
    @Override
    public void onCameraIdle() {
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
//        requestPhotoListWithDegree(null, null);

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
    public void onCameraMoveStarted(int i) {

    }
}

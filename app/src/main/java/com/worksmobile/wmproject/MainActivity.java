package com.worksmobile.wmproject;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.service.MediaStoreJobService;
import com.worksmobile.wmproject.service.MediaStoreService;


public class MainActivity extends AppCompatActivity {

    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 777;
    private static final String SHARED_PREFERENCES_NAME = "TokenStatePreference";
    private static final String TOKEN_STATE = "TOKEN_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    private static final String TAG = "MAIN_ACTIVITY";

    private String AUTH_URL;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + getString(R.string.client_id) + "&" +
                "response_type=code&" +
                "access_type=offline&" +
                "prompt=consent&" +
                "scope=https://www.googleapis.com/auth/drive&" +
                "redirect_uri=com.worksmobile.wmproject:/oauth2callback";

        findViewById(R.id.start_service).setOnClickListener((View view) -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(AUTH_URL));

            startActivity(intent);
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel("WM_PROJECT", "project1", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
//                notificationManager.deleteNotificationChannel("WM");
//                notificationManager.deleteNotificationChannel("project");
            }
        }


        checkPermission();
    }

    private void getAuthCode() {
        Uri uri = getIntent().getData();
        if (uri != null)
            System.out.println("URI : " + uri.toString());
        if (uri != null && uri.toString().startsWith("com.worksmobile.wmproject:/oauth2callback")) {
            // use the parameter your API exposes for the code (mostly it's "code")
            String code = uri.getQueryParameter("code");
            if (code != null) {
                System.out.println("TEST: " + code);
                requestToken(code);
            } else if (uri.getQueryParameter("error") != null) {
                // show an error message here
            }
        }
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Intent.ACTION_VIEW:
                        if (!intent.hasExtra(USED_INTENT)) {
                            getAuthCode();
                            intent.putExtra(USED_INTENT, true);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        System.out.println("Onstart");
        super.onStart();
        checkIntent(getIntent());
    }

    private void requestToken(String code) {
        DriveHelper driveHelper = new DriveHelper(getString(R.string.client_id), null, this);
        driveHelper.getToken(new TokenCallback() {
            @Override
            public void onSuccess(Token token) {
                if (token != null)
                    persistToken(token);
            }

            @Override
            public void onFailure(String msg) {
                Log.e(TAG, msg);
            }
        }, code);

    }


    private void persistToken(@NonNull Token token) {
        Gson gson = new Gson();
        String tokenJson = gson.toJson(token);

        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(TOKEN_STATE, tokenJson)
                .apply();
    }


    private void setJobSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaStoreJobService.scheduleJob(this);
        } else {
            final Intent intent = new Intent(MainActivity.this, MediaStoreService.class);
            startService(intent);
        }
    }


    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                setJobSchedule();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
            }

        } else {
            setJobSchedule();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setJobSchedule();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

package com.worksmobile.wmproject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.io.File;

public class MediaStoreEventService extends Service {

    private int storageCount;
    private Cursor countCursor;
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private MediaStorageObserver mediaStorageObserver;
    private AuthState authState;

    public static final String TEST_FOLDER_ID = "0BzHAMvdMNu8aSFNBVFdhWldFM2c";
    private DriveHelper mDriveHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        countCursor = getContentResolver().query(uri,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        registerObserver();

        System.out.println("SERVICE CREATE");


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (countCursor != null) {
            countCursor.moveToFirst();
            storageCount = countCursor.getInt(0);
        }

        authState = restoreAuthState();

        if (mDriveHelper == null)
            mDriveHelper = new DriveHelper(getString(R.string.client_id),getString(R.string.client_secret));


//        mDriveHelper.setAccessToken(intent.getStringExtra("AUTH_CODE"));


        mDriveHelper.setAccessToken(authState.getAccessToken());
        System.out.println("스토리지 갯수 : " + storageCount);
        System.out.println("TOKEN TEST : " + authState.getAccessToken());
        mDriveHelper.listFiles("root", null);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterObserver();
        System.out.println("Service Destroy");
        super.onDestroy();
    }

    private void persistAuthState(@NonNull AuthState authState) {
        getSharedPreferences("AuthStatePreference", Context.MODE_PRIVATE).edit()
                .putString("AUTH_STATE", authState.toJsonString())
                .apply();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getSharedPreferences("AuthStatePreference", Context.MODE_PRIVATE)
                .getString("AUTH_STATE", null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerObserver() {
        if (mediaStorageObserver == null) {
            mediaStorageObserver = new MediaStorageObserver(new Handler());
            getContentResolver().registerContentObserver(uri, true, mediaStorageObserver);
        }
    }

    private void unregisterObserver() {
        if (getContentResolver() != null) {
            getContentResolver().unregisterContentObserver(mediaStorageObserver);
        }
        if (mediaStorageObserver != null) {
            mediaStorageObserver = null;
        }
    }

    private class MediaStorageObserver extends ContentObserver {
        MediaStorageObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            int previousCount = storageCount;

            countCursor = getContentResolver().query(uri,
                    new String[]{"count(*) AS count"},
                    null,
                    null,
                    null);

            if (countCursor != null) {
                countCursor.moveToFirst();
                storageCount = countCursor.getInt(0);
            }

            if (storageCount > previousCount) {
                System.out.println("사진추가");
                createNotification();
            } else {
                System.out.println("사진 추가는 아님");
            }

            System.out.println("SERVICE : 스토리지 갯수 : " + storageCount);
            System.out.println(authState.getNeedsTokenRefresh());
//            refreshToken();

        }

    }

    private void refreshToken() {
        AuthorizationService service = new AuthorizationService(this);
        service.performTokenRequest(authState.createTokenRefreshRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                if (exception != null) {
                    Log.w("TAG", "Token Exchange failed", exception);
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception);
//                        persistAuthState(authState);
                        Log.i("TAG", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                    }
                }
            }
        });


    }


    /**
     * Android O 부터는 NotificationChannel 사용해야한다. 아마도 버전 분기가 필요할듯 하다.
     */
    private void createNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

//        Intent intent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!")
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_HIGH);

        if (notificationManager != null) {
            notificationManager.notify(0, mBuilder.build());
//            startForeground(100, mBuilder.build());
        }


        getLastPicture();
    }

    private void getLastPicture() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

// Put it in the image view
        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            System.out.println("LOCATION : " + imageLocation);
            mDriveHelper.uploadFile(new File(imageLocation), TEST_FOLDER_ID, null);
        }
    }


}

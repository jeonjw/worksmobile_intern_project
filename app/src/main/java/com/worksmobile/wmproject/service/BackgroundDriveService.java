package com.worksmobile.wmproject.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.worksmobile.wmproject.ContractDB;
import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.StateCallback;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.io.File;


public class BackgroundDriveService extends Service {

    private AuthState authState;

    public static final String TEST_FOLDER_ID = "0BzHAMvdMNu8aSFNBVFdhWldFM2c";
    private DriveHelper mDriveHelper;
    private DBHelpler dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        authState = restoreAuthState();
        dbHelper = new DBHelpler(this);

        if (mDriveHelper == null)
            mDriveHelper = new DriveHelper(getString(R.string.client_id), getString(R.string.client_secret));

        if (authState != null) {
            if (authState.getNeedsTokenRefresh())
                refreshToken();
            mDriveHelper.setAccessToken(authState.getAccessToken());
        }


        printDBList();
        createNotification();
        uploadToDrive();


        return START_NOT_STICKY;
    }

    private void printDBList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(ContractDB.SQL_SELECT, null)) {
            while (cursor.moveToNext()) {
                System.out.println("CURSOR " + cursor.getInt(0));
                System.out.println("CURSOR " + cursor.getString(1));
            }
        }


    }


    @Override
    public void onDestroy() {
        System.out.println("Upload Service Destroy");
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

    private void refreshToken() {
        AuthorizationService service = new AuthorizationService(this);
        service.performTokenRequest(authState.createTokenRefreshRequest(), (tokenResponse, exception) -> {
            if (exception != null) {
                Log.w("TAG", "Token Exchange failed", exception);
            } else {
                if (tokenResponse != null) {
                    authState.update(tokenResponse, null);
                    persistAuthState(authState);
                    Log.i("TAG", String.format("Token Refresh Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                }
            }
        });
        service.dispose();
    }

    private void createNotification() {
        NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification notification = new Notification.Builder(this, "WM")
                    .setContentTitle("Some Message")
                    .setContentText("You've received new messages!")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            startForeground(100, notification);

        } else {
            System.out.println("버전 오레오 미만");

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.android)
                            .setContentTitle("My notification")
                            .setContentText("Hello World!")
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setPriority(Notification.PRIORITY_HIGH);

            if (notificationManager != null) {
                notificationManager.notify(0, mBuilder.build());
            }
        }
    }

    private void uploadToDrive() {
        DBHelpler dbHelper = new DBHelpler(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String imageLocation;

        try (Cursor cursor = db.rawQuery(ContractDB.SQL_SELECT, null)) {
            while (cursor.moveToNext()) {
                imageLocation = cursor.getString(1);
                db.delete(ContractDB.TBL_CONTACT, ContractDB.COL_LOACTION + "=?", new String[]{imageLocation});
                createUploadRequest(imageLocation);
            }
        }
        stopSelf();
    }

    private void createUploadRequest(String imageLocation) {
        mDriveHelper.uploadFile(new File(imageLocation), TEST_FOLDER_ID, new StateCallback() {
            @Override
            public void onSuccess() {
                System.out.println("업로드 성공");
            }

            @Override
            public void onFailure(String msg) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(ContractDB.COL_LOACTION, imageLocation);

                db.insert(ContractDB.TBL_CONTACT, null, values);
                System.out.println("업로드 실패");
            }
        });

    }

}
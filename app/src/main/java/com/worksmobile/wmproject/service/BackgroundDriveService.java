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

import com.google.gson.Gson;
import com.worksmobile.wmproject.ContractDB;
import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.retrofit_object.Token;

import java.io.File;


public class BackgroundDriveService extends Service {

    private Token token;

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
        token = restoreAuthState();
        dbHelper = new DBHelpler(this);

        if (mDriveHelper == null)
            mDriveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null);

        System.out.println("TOKEN : " + token.getAccessToken());
        mDriveHelper.setToken(token);

        if (token != null) {
            if (token.getNeedsTokenRefresh()) {
                System.out.println("TOKEN 만료");
                mDriveHelper.refreshToken(new TokenCallback() {
                    @Override
                    public void onSuccess(Token token) {
//                        printDBList();
//                        createNotification();
//                        uploadToDrive();
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                });
            }
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

    private void persistAuthState(@NonNull Token token) {
        Gson gson = new Gson();
        String tokenJson = gson.toJson(token);

        getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE).edit()
                .putString("TOKEN_STATE", tokenJson)
                .apply();
    }

    @Nullable
    private Token restoreAuthState() {
        Gson gson = new Gson();
        String jsonString = getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE)
                .getString("TOKEN_STATE", null);

        System.out.println("JSON STRING" + jsonString);

        return gson.fromJson(jsonString, Token.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        mDriveHelper.uploadFile(new File(imageLocation), new StateCallback() {
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
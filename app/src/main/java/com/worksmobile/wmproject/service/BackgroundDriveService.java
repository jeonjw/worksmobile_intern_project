package com.worksmobile.wmproject.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.worksmobile.wmproject.MainActivity;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.callback.UploadCallback;
import com.worksmobile.wmproject.retrofit_object.Token;

import java.io.File;
import java.util.Locale;


public class BackgroundDriveService extends Service {

    public static final String NOTIFICATION_TITLE = "네이버 클라우드";
    public static final String NOTIFICATION_UPLOAD_SUCCESS_MESSAGE = "1개의 파일이 안전하게 보관되었습니다.";
    public static final String NOTIFICATION_UPLOAD_SUCCESS_MESSAGE_1 = "%d개의 파일이 안전하게 보관되었습니다.";
    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private Cursor uploadCursor;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int currentProgress;
    private PendingIntent pendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Token token = restoreAuthState();
        dbHelper = new DBHelpler(this);

        if (driveHelper == null)
            driveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null);

        System.out.println("TOKEN : " + token.getAccessToken());
        driveHelper.setToken(token);

        if (token != null) {
            if (token.getNeedsTokenRefresh()) {
                driveHelper.refreshToken(new TokenCallback() {
                    @Override
                    public void onSuccess(Token token) {
                        persistAuthState(token);
                        printDBList();
                        uploadCursor = findUploadList();
                        if (uploadCursor != null && uploadCursor.getCount() > 0) {
                            createNotification(uploadCursor.getCount());
                            createUploadRequest(uploadCursor);
                        }
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                });
            } else {
                printDBList();
                uploadCursor = findUploadList();
                if (uploadCursor != null && uploadCursor.getCount() > 0) {
                    createNotification(uploadCursor.getCount());
                    createUploadRequest(uploadCursor);
                }

            }
        }

        return START_NOT_STICKY;
    }

    private void printDBList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(ContractDB.SQL_SELECT_ALL, null)) {
            while (cursor.moveToNext()) {
                System.out.println("CURSOR " + cursor.getInt(0) + " Location : " + cursor.getString(1) + " STATUS : " + cursor.getString(2));
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

    private void createNotification(int progressMax) {
        notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder =
                    new NotificationCompat.Builder(this, "WM_PROJECT")
                            .setVibrate(new long[]{0})
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(NOTIFICATION_UPLOAD_SUCCESS_MESSAGE)
                            .setProgress(progressMax, 0, false)
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_launcher_foreground);

            startForeground(100, notificationBuilder.build());
        } else {
            notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.android)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(NOTIFICATION_UPLOAD_SUCCESS_MESSAGE)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setProgress(progressMax, 0, false)
                            .setContentIntent(pendingIntent)
                            .setPriority(Notification.PRIORITY_HIGH);

            notificationManager.notify(100, notificationBuilder.build());
        }

    }

    private Cursor findUploadList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(ContractDB.SQL_SELECT_UPLOAD, null);
    }

    private void sendUploadFinishNotification(int successCount) {
        String successMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOAD_SUCCESS_MESSAGE_1, successCount);
        notificationBuilder = new NotificationCompat.Builder(BackgroundDriveService.this, "WM_PROJECT")
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(successMessage)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationManager.notify(200, notificationBuilder.build());
    }

    private void createUploadRequest(Cursor uploadCursor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

//        driveHelper.uploadFileSync(uploadCursor, db, new UploadCallback() {
//
//            @Override
//            public void onSuccess(String databaseID) {
//                currentProgress++;
//
//                ContentValues values = new ContentValues();
//                values.put("STATUS", "UPLOADED");
//
//                db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
//                notificationBuilder.setProgress(uploadCursor.getCount(), currentProgress, false);
//                notificationBuilder.setContentText("ProgressBar : " + currentProgress);
//                notificationManager.notify(100, notificationBuilder.build());
//
//                System.out.println("요청 완료");
//
//                if (currentProgress == uploadCursor.getCount()) {
//                    sendUploadFinishNotification(currentProgress);
//                    stopSelf();
//                }
//            }
//
//            @Override
//            public void onFailure(String msg, String databaseID) {
//                ContentValues values = new ContentValues();
//                values.put("STATUS", "UPLOAD");
//                db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
//                System.out.println("업로드 실패");
//            }
//        });


        while (uploadCursor.moveToNext()) {
            String databaseID = uploadCursor.getString(0);
            String imageLocation = uploadCursor.getString(1);
            ContentValues values = new ContentValues();
            values.put("STATUS", "UPLOADING");
            db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});

            driveHelper.uploadFile(new File(imageLocation), databaseID, new UploadCallback() {
                @Override
                public void onSuccess(String databaseID) {
                    currentProgress++;

                    ContentValues values = new ContentValues();
                    values.put("STATUS", "UPLOADED");

                    db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
                    notificationBuilder.setProgress(uploadCursor.getCount(), currentProgress, false);
                    notificationBuilder.setContentText("ProgressBar : " + currentProgress);
                    notificationManager.notify(100, notificationBuilder.build());

                    System.out.println("요청 완료");

                    if (currentProgress == uploadCursor.getCount()) {
                        sendUploadFinishNotification(currentProgress);
                        stopSelf();
                    }
                }

                @Override
                public void onFailure(String msg, String databaseID) {
                    ContentValues values = new ContentValues();
                    values.put("STATUS", "UPLOAD");
                    db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
                    System.out.println("업로드 실패");
                }
            });
        }
    }

}
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.worksmobile.wmproject.ContractDB;
import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.DriveUtils;
import com.worksmobile.wmproject.MainActivity;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;


public class BackgroundDriveService extends Service {

    public static final String NOTIFICATION_TITLE = "네이버 클라우드";
    private static final String SUCCESS = "SUCCESS";
    public static final String NOTIFICATION_UPLOAD_SUCCESS_MESSAGE = "%d개의 파일이 안전하게 보관되었습니다.";
    public static final String NOTIFICATION_UPLOADING_MESSAGE = "파일을 올리고 있습니다 (%d/%d)";
    public static final String NOTIFICATION_UPLOAD_TRY_MESSAGE = "%d개의 파일이 업로드 시도중";
    private static final int PROGRESS_NOTIFICATION_ID = 100;
    private static final int FINISH_NOTIFICATION_ID = 200;
    private static final int UPLOAD_SUCCESS = 777;
    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private Cursor uploadCursor;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int currentProgress;
    private int totalUploadCount;
    private PendingIntent pendingIntent;

    private boolean isProgressNotificationRunning;
    private UploadHandlerThread uploadHandlerThread;

    private Handler mainThreadHandler;


    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand onStartCommand onStartCommand onStartCommand");
        dbHelper = new DBHelpler(this);

        if (driveHelper == null)
            driveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null);

        Token token = restoreAuthState();
        driveHelper.setToken(token);

        if (token != null) {
            if (token.getNeedsTokenRefresh()) {
                driveHelper.refreshToken(new TokenCallback() {
                    @Override
                    public void onSuccess(Token token) {
                        persistAuthState(token);
                        printDBList();
                        findUploadList();
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
                findUploadList();
                if (uploadCursor != null && uploadCursor.getCount() > 0) {
                    totalUploadCount += uploadCursor.getCount();
                    createNotification(uploadCursor.getCount());
                    createUploadRequest(uploadCursor);
                }

            }
        }

//        System.out.println("Total Count : " + totalUploadCount + "Cursor Count : " + uploadCursor.getCount());

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
        if (isProgressNotificationRunning) {
            return;
        }

        notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        String tryMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOAD_TRY_MESSAGE, progressMax);

        Intent intent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder =
                    new NotificationCompat.Builder(this, "WM_PROJECT")
                            .setVibrate(new long[]{0})
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(tryMessage)
                            .setProgress(progressMax, 0, false)
                            .setContentIntent(pendingIntent)
                            .setSmallIcon(R.drawable.ic_launcher_foreground);

            startForeground(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
            isProgressNotificationRunning = true;
        } else {
            notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.android)
                            .setContentTitle(NOTIFICATION_TITLE)
                            .setContentText(tryMessage)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setProgress(progressMax, 0, false)
                            .setContentIntent(pendingIntent)
                            .setPriority(Notification.PRIORITY_HIGH);

            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
            isProgressNotificationRunning = true;
        }

    }

    private void findUploadList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        uploadCursor = db.rawQuery(ContractDB.SQL_SELECT_UPLOAD, null);

        while (uploadCursor.moveToNext()) {
            String databaseID = uploadCursor.getString(0);
            ContentValues values = new ContentValues();
            values.put("STATUS", "UPLOADING");
            db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
        }
    }

    private void sendUploadFinishNotification(int successCount) {
        String successMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOAD_SUCCESS_MESSAGE, successCount);
        notificationBuilder = new NotificationCompat.Builder(BackgroundDriveService.this, "WM_PROJECT")
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(successMessage)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationManager.notify(FINISH_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createUploadRequest(Cursor uploadCursor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        uploadHandlerThread = new UploadHandlerThread("UploadHandlerThread");
        uploadHandlerThread.start();

        if (uploadCursor.moveToFirst()) {
            do {
                String databaseID = uploadCursor.getString(0);
                String imageLocation = uploadCursor.getString(1);
                Call<UploadResult> call = driveHelper.createUploadCall(imageLocation);

                uploadHandlerThread.executeUpload(call);

            } while (uploadCursor.moveToNext());
        }

        mainThreadHandler = new Handler(uploadHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPLOAD_SUCCESS:

                        break;
                }
            }
        };
    }

    class UploadHandlerThread extends HandlerThread {
        private static final int UPLOAD = 700;
        private Handler handler;

        public UploadHandlerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case UPLOAD:
                            Call<UploadResult> call = (Call<UploadResult>) msg.obj;

                            try {
                                Response<UploadResult> response = call.execute();
                                String message = DriveUtils.printResponse("uploadFile", response);
                                if (message == SUCCESS) { //성공
                                    mainThreadHandler.sendEmptyMessage(UPLOAD_SUCCESS);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
        }

        public void executeUpload(Call<UploadResult> call) {
            Message message = handler.obtainMessage(UPLOAD, call);
            handler.sendMessage(message);
        }
    }

}


//        driveHelper.uploadFileSync(uploadCursor, new UploadCallback() {
//
//            @Override
//            public void onSuccess(String databaseID) {
//                currentProgress++;
//
//                ContentValues values = new ContentValues();
//                values.put("STATUS", "UPLOADED");
//                db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
//
//                String tryMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOADING_MESSAGE, currentProgress, totalUploadCount);
//                notificationBuilder.setProgress(totalUploadCount, currentProgress, false);
//                notificationBuilder.setContentText(tryMessage);
//                notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
//
//                System.out.println("요청 완료");
//
//                if (currentProgress == totalUploadCount) {
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
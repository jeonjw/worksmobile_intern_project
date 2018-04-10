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
import com.worksmobile.wmproject.MainThreadHandler;
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
    public static final String NOTIFICATION_UPLOAD_SUCCESS_MESSAGE = "%d개의 파일이 안전하게 보관되었습니다.";
    public static final String NOTIFICATION_UPLOAD_FAIL_MESSAGE = "%d개의 파일을 업로드 실패했습니다.";
    public static final String NOTIFICATION_UPLOADING_MESSAGE = "파일을 올리고 있습니다 (%d/%d)";
    public static final String NOTIFICATION_UPLOAD_TRY_MESSAGE = "%d개의 파일이 업로드 시도중";
    private static final int PROGRESS_NOTIFICATION_ID = 100;
    private static final int FINISH_NOTIFICATION_ID = 200;
    public static final int UPLOAD_REQUEST = 700;
    public static final int UPLOAD_FINISH = 701;
    public static final int UPLOAD_FAIL = 702;
    public static final int QUERY = 703;
    private static final int QUERY_FINISH = 704;

    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private Cursor uploadCursor;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int currentProgress;
    private int uploadFailCount;
    private int totalUploadCount;
    private PendingIntent pendingIntent;
    private boolean isProgressNotificationRunning;
    private UploadHandlerThread handlerThread;
    private Handler mainThreadHandler = new MainThreadHandler(this);
    private Token token;

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
        dbHelper = new DBHelpler(this);
        driveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null, this);

        token = restoreAuthState();
        driveHelper.setToken(token);

        handlerThread = new UploadHandlerThread("UploadHandlerThread");
        handlerThread.start();
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UPLOAD_FINISH:
                System.out.println("요청완료");
                handleUploadResult(UPLOAD_FINISH);
                break;

            case UPLOAD_FAIL:
                System.out.println("요청실패");
                handleUploadResult(UPLOAD_FAIL);
                break;

            case QUERY_FINISH:
                if (uploadCursor != null && uploadCursor.getCount() > 0) {
                    totalUploadCount += uploadCursor.getCount();
                    createNotification(uploadCursor.getCount());
                    createUploadRequest(uploadCursor);
                }
                break;
        }
    }

    private void handleUploadResult(int uploadStatus) {

        System.out.println("Current Thread : " + Thread.currentThread().getName());
        currentProgress++;

        if (uploadStatus == UPLOAD_FINISH) {
            String tryMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOADING_MESSAGE, currentProgress, totalUploadCount);
            notificationBuilder.setProgress(totalUploadCount, currentProgress, false);
            notificationBuilder.setContentText(tryMessage);
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
        } else if (uploadStatus == UPLOAD_FAIL) {
            uploadFailCount++;
        }

        if (currentProgress == totalUploadCount) {
            sendUploadFinishNotification();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (token != null) {
            if (token.getNeedsTokenRefresh()) {
                driveHelper.refreshToken(new TokenCallback() {
                    @Override
                    public void onSuccess(Token token) {
                        persistAuthState(token);
                        printDBList();
                        handlerThread.sendUploadListQuery();
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                });
            } else {
                printDBList();
                handlerThread.sendUploadListQuery();
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
        printDBList();
        System.out.println("Upload Service Destroy");
        handlerThread.quit();
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

    private void sendUploadFinishNotification() {
        String notificationMessage;
        if (uploadFailCount == 0) {
            notificationMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOAD_SUCCESS_MESSAGE, currentProgress);
        } else {
            notificationMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOAD_FAIL_MESSAGE, uploadFailCount);
        }

        notificationBuilder = new NotificationCompat.Builder(BackgroundDriveService.this, "WM_PROJECT")
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(notificationMessage)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationManager.notify(FINISH_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createUploadRequest(Cursor uploadCursor) {
        if (uploadCursor.moveToFirst()) {
            do {
                int databaseID = uploadCursor.getInt(0);
                String location = uploadCursor.getString(1);
                handlerThread.sendUploadRequest(databaseID, location);
            } while (uploadCursor.moveToNext());
        }
    }

    class UploadHandlerThread extends HandlerThread {

        private Handler handler;
        private Call<UploadResult> call;
        private boolean hasExceptionOccured;

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
                        case UPLOAD_REQUEST:
                            String location = (String) msg.obj;
                            executeUploadRequest(location);
                            break;

                        case QUERY:
                            createUploadList();
                            break;

                        case UPLOAD_FINISH: //ProgressRequest 에서 네트워크 문제로 While문 break당했을때도 여기로 빠지게된다. break당하면 실패인데 finish 후 성공 처리되니까 추 후 에러처리 하기.
                            mainThreadHandler.sendEmptyMessage(UPLOAD_FINISH);
                            String uploadedFileLocation = (String) msg.obj;
                            ContentValues values = new ContentValues();
                            values.put("STATUS", "UPLOADED");
                            dbHelper.getWritableDatabase().update(ContractDB.TBL_CONTACT, values, "LOCATION=?", new String[]{uploadedFileLocation});
                            break;
                    }
                }
            };
        }

        public void sendUploadRequest(int databaseID, String location) {
            Message message = handler.obtainMessage(UPLOAD_REQUEST, location);
            message.arg1 = databaseID;
            handler.sendMessage(message);
        }

        private void executeUploadRequest(String location) {
            if (!hasExceptionOccured) {
                call = driveHelper.createUploadCall(location, handler);
                try {
                    System.out.println("요청");
                    Response<UploadResult> response = call.execute();

                    DriveUtils.printResponse("uploadFile", response);
                } catch (IOException e) {
                    hasExceptionOccured = true;
                    e.printStackTrace();
                }
            } else {
                ContentValues values = new ContentValues();
                values.put("STATUS", "UPLOAD");
                dbHelper.getWritableDatabase().update(ContractDB.TBL_CONTACT, values, "LOCATION=?", new String[]{location});
                mainThreadHandler.sendEmptyMessage(UPLOAD_FAIL);
            }
        }

        public void sendUploadListQuery() {
            handler.sendEmptyMessage(QUERY);
        }

        private void createUploadList() {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            uploadCursor = db.rawQuery(ContractDB.SQL_SELECT_UPLOAD, null);

            while (uploadCursor.moveToNext()) {
                String dbID = uploadCursor.getString(0);
                ContentValues values = new ContentValues();
                values.put("STATUS", "UPLOADING");
                db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{dbID});
            }
            mainThreadHandler.sendEmptyMessage(QUERY_FINISH);
        }
    }
}
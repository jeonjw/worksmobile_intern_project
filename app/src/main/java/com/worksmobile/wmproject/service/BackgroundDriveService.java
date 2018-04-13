package com.worksmobile.wmproject.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import com.worksmobile.wmproject.ui.MainActivity;
import com.worksmobile.wmproject.MainThreadHandler;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;


public class BackgroundDriveService extends Service {
    private static final int PROGRESS_NOTIFICATION_ID = 100;
    private static final int FINISH_NOTIFICATION_ID = 200;
    public static final int READY = 300;
    public static final int UPLOAD_REQUEST = 700;
    public static final int UPLOAD_SUCCESS = 701;
    public static final int UPLOAD_FAIL = 702;
    public static final int UPLOAD_FAIL_400 = 703;
    public static final int UPLOAD_FAIL_EXCEPTION = 704;
    public static final int UPLOAD_FAIL_FILE_NOT_FOUND = 705;
    public static final int UPLOAD_REQUEST_FINISH = 710;
    public static final int QUERY = 706;
    private static final int QUERY_FINISH = 707;
    private static final int TOKEN_REFRESH = 708;

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
    private Handler mainThreadHandler;


    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
        mainThreadHandler = new MainThreadHandler(this);
        dbHelper = new DBHelpler(this);

        driveHelper = new DriveHelper(this);
        handlerThread = new UploadHandlerThread("UploadHandlerThread");
        handlerThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        printDBList();
        if (isProgressNotificationRunning)
            handlerThread.sendQueryRequest();

        return START_NOT_STICKY;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case READY:
                handlerThread.sendQueryRequest();
                break;
            case UPLOAD_SUCCESS:
                handleUploadResult(UPLOAD_SUCCESS);
                break;

            case UPLOAD_FAIL:
                handleUploadResult(UPLOAD_FAIL);
                break;

            case QUERY_FINISH:
                if (uploadCursor != null && uploadCursor.getCount() > 0) {
                    totalUploadCount += uploadCursor.getCount();
                    createNotification(uploadCursor.getCount());
                    createUploadRequest(uploadCursor);
                } else if (uploadCursor != null && uploadCursor.getCount() == 0 && totalUploadCount == 0) {
                    startForeground(999, new Notification()); //fakeStartForeground
                    stopSelf();
                }
                break;

            case UPLOAD_REQUEST_FINISH:
                sendUploadFinishNotification();
                stopSelf();
                break;
        }
    }

    private void handleUploadResult(int uploadStatus) {
        currentProgress++;
        if (uploadStatus == UPLOAD_SUCCESS) {
            String tryMessage = String.format(Locale.KOREA, getString(R.string.notification_uploading_message), currentProgress, totalUploadCount);
            notificationBuilder.setProgress(totalUploadCount, currentProgress, false);
            notificationBuilder.setContentText(tryMessage);
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
        } else if (uploadStatus == UPLOAD_FAIL) {
            uploadFailCount++;
        }


        System.out.println("COUNT : " + currentProgress + "TOTAL : " + totalUploadCount);
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
        dbHelper.closeDB();
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
        String tryMessage = String.format(Locale.KOREA, getString(R.string.notification_upload_try_message), progressMax);

        Intent intent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder =
                    new NotificationCompat.Builder(this, "WM_PROJECT")
                            .setVibrate(new long[]{0})
                            .setContentTitle(getString(R.string.notification_title))
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
                            .setContentTitle(getString(R.string.notification_title))
                            .setContentText(tryMessage)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setProgress(progressMax, 0, false)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(Notification.PRIORITY_LOW);

            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
            isProgressNotificationRunning = true;
        }
    }

    private void sendUploadFinishNotification() {
        String notificationMessage;
        if (uploadFailCount == 0) {
            notificationMessage = String.format(Locale.KOREA, getString(R.string.notification_upload_success_message), currentProgress);
        } else {
            notificationMessage = String.format(Locale.KOREA, getString(R.string.notification_upload_fail_message), uploadFailCount);
        }


//        notificationBuilder = new NotificationCompat.Builder(BackgroundDriveService.this, "WM_PROJECT")
//                .setContentTitle(getString(R.string.notification_title))
//                .setContentText(notificationMessage)
//                .setContentIntent(pendingIntent)
//                .setSmallIcon(R.drawable.ic_launcher_foreground);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
            notificationBuilder = new NotificationCompat.Builder(BackgroundDriveService.this, "WM_PROJECT")
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(notificationMessage)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_launcher_foreground);
        } else {
            notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.android)
                            .setContentTitle(getString(R.string.notification_title))
                            .setContentText(notificationMessage)
                            .setPriority(Notification.PRIORITY_LOW)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setContentIntent(pendingIntent);

        }
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID);
        notificationManager.notify(FINISH_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createUploadRequest(Cursor uploadCursor) {
        if (uploadCursor.moveToFirst()) {
            handlerThread.removeUploadFinishMessage();
            do {
                int databaseID = uploadCursor.getInt(0);
                String location = uploadCursor.getString(1);
                handlerThread.sendUploadRequest(databaseID, location);
            } while (uploadCursor.moveToNext());

            handlerThread.notifyUploadFinish();
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
                    String location;
                    switch (msg.what) {
                        case UPLOAD_REQUEST:
                            location = (String) msg.obj;
                            executeUpload(location);
                            break;
                        case UPLOAD_SUCCESS:
                            mainThreadHandler.sendEmptyMessage(UPLOAD_SUCCESS);

                            location = (String) msg.obj;
                            dbHelper.updateDB("STATUS", "UPLOADED", "LOCATION", location);
                            break;
                        case UPLOAD_FAIL:
                            Message message = handler.obtainMessage(UPLOAD_FAIL);
                            message.arg1 = msg.arg1;
                            mainThreadHandler.sendMessage(message);

                            location = (String) msg.obj;
                            dbHelper.updateDB("STATUS", "UPLOAD", "LOCATION", location);
                            break;
                        case QUERY:
                            createUploadList();
                            break;
                        case UPLOAD_REQUEST_FINISH:
                            mainThreadHandler.sendEmptyMessage(UPLOAD_REQUEST_FINISH);
                            break;
                    }
                }
            };
            Message message = handler.obtainMessage(READY);
            mainThreadHandler.sendMessageAtFrontOfQueue(message);
        }

        public void sendUploadRequest(int databaseID, String location) {
            Message message = handler.obtainMessage(UPLOAD_REQUEST, location);
            message.arg1 = databaseID;
            handler.sendMessage(message);
        }

        public void sendQueryRequest() {
            Message message = handler.obtainMessage(QUERY);
            handler.sendMessageAtFrontOfQueue(message);
        }

        public void sendTokenRefreshRequest() {
            Message message = handler.obtainMessage(TOKEN_REFRESH);
            handler.sendMessageAtFrontOfQueue(message);
        }

        public void notifyUploadFinish() {
            handler.sendEmptyMessage(UPLOAD_REQUEST_FINISH);
        }

        public void removeUploadFinishMessage() {
            handler.removeMessages(UPLOAD_REQUEST_FINISH);
        }

        private void executeUpload(String location) {
            if (!hasExceptionOccured) {
                call = driveHelper.createUploadCall(location, handler);
                if (call == null) {
                    dbHelper.deleteDB("LOCATION", location);

                    Message message = handler.obtainMessage(UPLOAD_FAIL);
                    message.arg1 = UPLOAD_FAIL_FILE_NOT_FOUND;

                    mainThreadHandler.sendMessage(message);
                    return;
                }
                try {
                    Response<UploadResult> response = call.execute();
                    DriveUtils.printResponse("uploadFile", response);

                    if (!response.isSuccessful()) {
                        Message message = handler.obtainMessage(UPLOAD_FAIL, location);
                        message.arg1 = UPLOAD_FAIL_400;
                        handler.removeMessages(UPLOAD_SUCCESS, location);
                        handler.sendMessageAtFrontOfQueue(message);
                    }
                } catch (IOException e) {
                    hasExceptionOccured = true;
                    e.printStackTrace();
                }
            } else {
                Message message = handler.obtainMessage(UPLOAD_FAIL, location);
                message.arg1 = UPLOAD_FAIL_EXCEPTION;
                handler.sendMessageAtFrontOfQueue(message);
            }
        }

        private void createUploadList() {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            uploadCursor = db.rawQuery(ContractDB.SQL_SELECT_UPLOAD, null);

            while (uploadCursor.moveToNext()) {
                String dbID = uploadCursor.getString(0);
                dbHelper.updateDB("STATUS", "UPLOADING", "_id", dbID);
            }
            mainThreadHandler.sendEmptyMessage(QUERY_FINISH);
        }
    }
}
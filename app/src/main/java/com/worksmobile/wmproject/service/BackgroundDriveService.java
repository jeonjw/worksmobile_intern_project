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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
    private static final String SUCCESS = "SUCCESS";
    public static final String NOTIFICATION_UPLOAD_SUCCESS_MESSAGE = "%d개의 파일이 안전하게 보관되었습니다.";
    public static final String NOTIFICATION_UPLOADING_MESSAGE = "파일을 올리고 있습니다 (%d/%d)";
    public static final String NOTIFICATION_UPLOAD_TRY_MESSAGE = "%d개의 파일이 업로드 시도중";
    private static final int PROGRESS_NOTIFICATION_ID = 100;
    private static final int FINISH_NOTIFICATION_ID = 200;
    private static final int UPLOAD_SUCCESS = 777;
    private static final int QUERY_FINISH = 778;

    private int firstAvailableCount;

    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private Cursor uploadCursor;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int currentProgress;
    private int totalUploadCount;
    private PendingIntent pendingIntent;
    private boolean isProgressNotificationRunning;
    private UploadHandlerThread handlerThread;
    private Handler mainThreadHandler = new MainThreadHandler(this);

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("BackGround SERVICE CREATE");
        handlerThread = new UploadHandlerThread("UploadHandlerThread");
        handlerThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tempTest();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void tempTest() {
        ConnectivityManager.NetworkCallback networkCallback;
        ConnectivityManager connectivityManager;

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                System.out.println("UPLOAD SERVICE onAvailable ");
                firstAvailableCount++;
                if (firstAvailableCount > 2)
                    handlerThread.quit();
            }

            @Override
            public void onLost(Network network) {
                System.out.println("UPLOAD SERVICE onLost ");

                System.out.println("QUIT");
                if(handlerThread != null){
                    handlerThread.quit();
                    handlerThread.interrupt();
                }

            }
        };
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UPLOAD_SUCCESS:
                System.out.println("요청완료");
                currentProgress++;

                String tryMessage = String.format(Locale.KOREA, NOTIFICATION_UPLOADING_MESSAGE, currentProgress, totalUploadCount);
                notificationBuilder.setProgress(totalUploadCount, currentProgress, false);
                notificationBuilder.setContentText(tryMessage);
                notificationManager.notify(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());

                if (currentProgress == totalUploadCount) {
                    sendUploadFinishNotification(currentProgress);
                    stopSelf();
                }
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand onStartCommand onStartCommand onStartCommand");
        dbHelper = new DBHelpler(this);

        if (driveHelper == null)
            driveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null, this);

        Token token = restoreAuthState();
        driveHelper.setToken(token);

        if (token != null) {
            if (token.getNeedsTokenRefresh()) {
                driveHelper.refreshToken(new TokenCallback() {
                    @Override
                    public void onSuccess(Token token) {
                        persistAuthState(token);
                        printDBList();
                        handlerThread.findUploadList();
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                });
            } else {
                printDBList();
                handlerThread.findUploadList();
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
        if (uploadCursor.moveToFirst()) {
            do {
                int databaseID = uploadCursor.getInt(0);
                String imageLocation = uploadCursor.getString(1);
                handlerThread.executeUpload(databaseID, imageLocation);
            } while (uploadCursor.moveToNext());
        }
    }

    class UploadHandlerThread extends HandlerThread {
        private static final int UPLOAD = 700;
        private static final int QUERY = 701;
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
                            String imageLocation = (String) msg.obj;
                            String databaseID = String.valueOf(msg.arg1);
                            Call<UploadResult> call = driveHelper.createUploadCall(imageLocation);

                            try {
                                System.out.println("요청");
                                Response<UploadResult> response = call.execute();

                                String message = DriveUtils.printResponse("uploadFile", response);
                                if (message == SUCCESS) {
                                    mainThreadHandler.sendEmptyMessage(UPLOAD_SUCCESS);
                                    ContentValues values = new ContentValues();
                                    values.put("STATUS", "UPLOADED");
                                    dbHelper.getWritableDatabase().update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
                                }
                            } catch (IOException e) {

                                ContentValues values = new ContentValues();
                                values.put("STATUS", "UPLOAD");
                                dbHelper.getWritableDatabase().update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{databaseID});
                                e.printStackTrace();
                            }
                            break;

                        case QUERY:
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            uploadCursor = db.rawQuery(ContractDB.SQL_SELECT_UPLOAD, null);

                            while (uploadCursor.moveToNext()) {
                                String dbID = uploadCursor.getString(0);
                                ContentValues values = new ContentValues();
                                values.put("STATUS", "UPLOADING");
                                db.update(ContractDB.TBL_CONTACT, values, "_id=?", new String[]{dbID});
                            }
                            mainThreadHandler.sendEmptyMessage(QUERY_FINISH);

                            break;
                    }
                }
            };
        }

        public void executeUpload(int databaseID, String imageLocation) {
            Message message = handler.obtainMessage(UPLOAD, imageLocation);
            message.arg1 = databaseID;
            handler.sendMessage(message);
        }

        public void findUploadList() {
            handler.sendEmptyMessage(QUERY);
        }
    }

}
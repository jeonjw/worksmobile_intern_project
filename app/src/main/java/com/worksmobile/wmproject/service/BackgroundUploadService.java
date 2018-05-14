package com.worksmobile.wmproject.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.worksmobile.wmproject.BackgroundServiceHandler;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.value_object.UploadResult;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.ui.MainActivity;
import com.worksmobile.wmproject.util.DriveUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Response;


public class BackgroundUploadService extends Service {

    private static final String TAG = "UPLOAD_SERVICE";
    private static final int PROGRESS_NOTIFICATION_ID = 100;
    private static final int FINISH_NOTIFICATION_ID = 200;
    public static final int READY = 300;
    public static final int UPLOAD_REQUEST = 700;
    public static final int UPLOAD_SUCCESS = 701;
    public static final int UPLOAD_FAIL = 702;
    public static final int UPLOAD_REQUEST_FINISH = 710;
    public static final int QUERY = 706;
    private static final int QUERY_FINISH = 707;
    private static final int FAKE_NOTIFICATION_ID = 999;

    private DriveHelper driveHelper;
    private List<FileStatus> uploadFileList;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private int currentProgress;
    private int uploadFailCount;
    private int totalUploadCount;
    private PendingIntent pendingIntent;
    private boolean isProgressNotificationRunning;
    private UploadHandlerThread handlerThread;
    private Handler mainThreadHandler;
    private AppDatabase appDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Background Service Create");
        appDatabase = AppDatabase.getDatabase(getBaseContext());
        mainThreadHandler = new BackgroundServiceHandler(this);
        driveHelper = new DriveHelper(this);
        handlerThread = new UploadHandlerThread("UploadHandlerThread");
        handlerThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isProgressNotificationRunning) {
            handlerThread.sendQueryRequest();
        }

        return START_NOT_STICKY;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
//            case READY:
//                handlerThread.sendQueryRequest();
//                break;
            case UPLOAD_SUCCESS:
                handleUploadResult(UPLOAD_SUCCESS);
                break;

            case UPLOAD_FAIL:
                handleUploadResult(UPLOAD_FAIL);
                break;

            case QUERY_FINISH:
                if (uploadFileList != null && uploadFileList.size() > 0) {
                    totalUploadCount += uploadFileList.size();
                    createNotification(uploadFileList.size());
                    createUploadRequest(uploadFileList);
                } else if (uploadFileList != null && uploadFileList.size() == 0 && totalUploadCount == 0) {
                    startForeground(FAKE_NOTIFICATION_ID, new Notification()); //fakeStartForeground
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

        Log.d(TAG, "COUNT : " + currentProgress + "TOTAL : " + totalUploadCount);
    }

    private void printDBWithRoom() {
        for (FileStatus fileStatus : appDatabase.fileDAO().getAll()) {
            Log.d(TAG, "CURSOR " + fileStatus.getId() + " LocationInfo : " + fileStatus.getLocation() + " STATUS : " + fileStatus.getStatus() + " DATE : " + fileStatus.getDate());
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Upload Service Destroy");
        handlerThread.quit();
        super.onDestroy();
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

        notificationBuilder = new NotificationCompat.Builder(this, "WM_PROJECT")
                        .setSmallIcon(R.drawable.ic_cloud)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setVibrate(new long[]{0})
                        .setContentTitle(getString(R.string.notification_title))
                        .setContentText(tryMessage)
                        .setAutoCancel(true)
                        .setProgress(progressMax, 0, false)
                        .setContentIntent(pendingIntent);

        startForeground(PROGRESS_NOTIFICATION_ID, notificationBuilder.build());
        isProgressNotificationRunning = true;
    }

    private void sendUploadFinishNotification() {
        String notificationMessage;
        if (uploadFailCount == 0) {
            notificationMessage = String.format(Locale.KOREA, getString(R.string.notification_upload_success_message), currentProgress);
        } else {
            notificationMessage = String.format(Locale.KOREA, getString(R.string.notification_upload_fail_message), uploadFailCount);
        }

        stopForeground(true);

        notificationBuilder = new NotificationCompat.Builder(BackgroundUploadService.this, "WM_PROJECT")
                .setSmallIcon(R.drawable.ic_cloud)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(notificationMessage)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.cancel(PROGRESS_NOTIFICATION_ID);
        notificationManager.notify(FINISH_NOTIFICATION_ID, notificationBuilder.build());
    }

    private void createUploadRequest(List<FileStatus> uploadFileList) {
        handlerThread.removeUploadFinishMessage();
        for (FileStatus fileStatus : uploadFileList) {
            handlerThread.sendUploadRequest(fileStatus);
        }
        handlerThread.notifyUploadFinish();
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
                    FileStatus status;
                    switch (msg.what) {
                        case UPLOAD_REQUEST:
                            status = (FileStatus) msg.obj;
                            executeUpload(status);
                            break;
                        case UPLOAD_SUCCESS:
                            mainThreadHandler.sendEmptyMessage(UPLOAD_SUCCESS);
                            status = (FileStatus) msg.obj;
                            status.setStatus("UPLOADED");
                            appDatabase.fileDAO().updateFileStatus(status);
                            break;
                        case UPLOAD_FAIL:
                            Message message = handler.obtainMessage(UPLOAD_FAIL);
                            message.arg1 = msg.arg1;
                            mainThreadHandler.sendMessage(message);

                            status = (FileStatus) msg.obj;
                            status.setStatus("UPLOAD");
                            appDatabase.fileDAO().updateFileStatus(status);
                            break;
//                        case QUERY:
//                            createUploadList();
//                            break;
                        case UPLOAD_REQUEST_FINISH:
                            mainThreadHandler.sendEmptyMessage(UPLOAD_REQUEST_FINISH);
                            break;
                    }
                }
            };
//            Message message = handler.obtainMessage(READY);
//            mainThreadHandler.sendMessageAtFrontOfQueue(message);
            createUploadList();
        }

        public void sendUploadRequest(FileStatus fileStatus) {
            Message message = handler.obtainMessage(UPLOAD_REQUEST, fileStatus);
            handler.sendMessage(message);
        }

        public void sendQueryRequest() {
            Message message = handler.obtainMessage(QUERY);
            handler.sendMessageAtFrontOfQueue(message);
        }

        public void notifyUploadFinish() {
            handler.sendEmptyMessage(UPLOAD_REQUEST_FINISH);
        }

        public void removeUploadFinishMessage() {
            handler.removeMessages(UPLOAD_REQUEST_FINISH);
        }

        private void executeUpload(FileStatus fileStatus) {
            if (!hasExceptionOccured) {
                call = driveHelper.createUploadCall(fileStatus, handler);
                if (call == null) {//파일이 존재하지 않을경우
                    appDatabase.fileDAO().deleteFileStatus(fileStatus);

                    Message message = handler.obtainMessage(UPLOAD_FAIL);
                    mainThreadHandler.sendMessage(message);
                    return;
                }
                try {
                    Response<UploadResult> response = call.execute();
                    DriveUtils.printResponse("uploadFile", response);

                    if (!response.isSuccessful()) {
                        Message message = handler.obtainMessage(UPLOAD_FAIL, fileStatus);
                        handler.removeMessages(UPLOAD_SUCCESS, fileStatus);
                        handler.sendMessageAtFrontOfQueue(message);
                    }
                } catch (IOException e) {
                    hasExceptionOccured = true;
                    e.printStackTrace();
                }
            } else {
                Message message = handler.obtainMessage(UPLOAD_FAIL, fileStatus);
                handler.sendMessageAtFrontOfQueue(message);
            }
        }

        private void createUploadList() {
            uploadFileList = appDatabase.fileDAO().getUploadFileList();
            for (FileStatus fileStatus : uploadFileList) {
                fileStatus.setStatus("UPLOADING");
                appDatabase.fileDAO().updateFileStatus(fileStatus);
            }

            mainThreadHandler.sendEmptyMessage(QUERY_FINISH);
        }
    }
}
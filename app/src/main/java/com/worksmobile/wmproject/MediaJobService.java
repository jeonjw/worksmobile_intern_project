package com.worksmobile.wmproject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaJobService extends JobService {

    private int storageCount;
    private Cursor countCursor;
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    private MediaStorageObserver mediaStorageObserver;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        countCursor = getContentResolver().query(uri,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        if (countCursor != null) {
            countCursor.moveToFirst();
            storageCount = countCursor.getInt(0);
        }

        System.out.println("스토리지 갯수 : " + storageCount);

        registerObserver();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("JOB 종료");
        unregisterObserver();
        return true;
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

            System.out.println(" JOB : 스토리지 갯수 : " + storageCount);
        }
    }

    private void createNotification() {
        NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification notification = new Notification.Builder(this, "WM")
                    .setContentTitle("Some Message")
                    .setContentText("You've received new messages!")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            notificationManager.notify(1, notification);

        } else {
            System.out.println("버전 오레오 미만");

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
        }


    }


}

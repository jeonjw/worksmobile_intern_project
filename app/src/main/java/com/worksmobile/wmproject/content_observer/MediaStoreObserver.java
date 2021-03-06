package com.worksmobile.wmproject.content_observer;


import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import com.worksmobile.wmproject.MyBroadcastReceiver;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaStoreObserver extends ContentObserver {

    private static final String TAG = "MEDIA_STORE_OBSERVER";
    private static final int CALLBACK_PRESENT_INTEGER = 0;

    private Uri EXTERNAL_CONTENT_URI;
    private Context context;
    private Handler handler;
    private Runnable broakdCastTask;
    int previousCount;

    public MediaStoreObserver(Handler handler, Context context, Uri contentUri) {
        super(handler);
        this.context = context;
        this.handler = handler;

        EXTERNAL_CONTENT_URI = contentUri;
        previousCount = getStorageCount();
    }


    @Override
    public void onChange(boolean selfChange) {
        int currentCount = getStorageCount();
        Log.d(TAG, "onChange Previous count : " + previousCount + " Current count : " + currentCount);

        if (currentCount > previousCount) {
            String lastPicturePath = getLastPictureLocation();
            if (isDownlodFileFromDrive(lastPicturePath)) {
                return;
            }

            Log.d(TAG, "Media 추가");
            DateFormat sdFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
            Date nowDate = new Date();
            String tempDate = sdFormat.format(nowDate);
            AppDatabase.getDatabase(context).fileDAO().insertFileStatus(new FileStatus(lastPicturePath, tempDate, "UPLOAD"));

            if (handler.hasMessages(CALLBACK_PRESENT_INTEGER) && broakdCastTask != null) {
                handler.removeCallbacks(broakdCastTask);
            }

            broakdCastTask = getBroadCastTask();
            handler.postDelayed(broakdCastTask, 3000);
        }
        previousCount = currentCount;

    }

    private int getStorageCount() {
        int count = -1;
        Cursor countCursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        if (countCursor != null) {
            countCursor.moveToFirst();
            count = countCursor.getInt(0);
        }
        countCursor.close();

        return count;
    }

    private boolean isDownlodFileFromDrive(String location) {
        return location.contains("/storage/emulated/0/DCIM/WorksDrive/");
    }

    private Runnable getBroadCastTask() {
        return new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(CALLBACK_PRESENT_INTEGER);
                Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
                intent.setClass(context, MyBroadcastReceiver.class);
                context.sendBroadcast(intent);
            }
        };
    }

    private String getLastPictureLocation() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_ADDED,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = context.getContentResolver()
                .query(EXTERNAL_CONTENT_URI, projection, null,
                        null, "date_added" + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(1);
        }

        cursor.close();
        return null;
    }
}
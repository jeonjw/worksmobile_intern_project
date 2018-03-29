package com.worksmobile.wmproject.android_job;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.worksmobile.wmproject.MyBroadCastReceiver;

import java.util.concurrent.TimeUnit;


public class MediaStoreJob extends Job {

    public static final String TAG = "MEDIA_STORE_JOB_TAG";

    private int storageCount;
    private Cursor countCursor;
    private ContentResolver contentResolver;
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    private MediaStorageObserver mediaStorageObserver;

    @Override
    protected void onCancel() {
        System.out.println("onCancel");
        unregisterObserver();
        super.onCancel();
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        contentResolver = getContext().getContentResolver();
        countCursor = contentResolver.query(uri,
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

        return Result.RESCHEDULE;
    }


    @Override
    protected void onReschedule(int newJobId) {
        System.out.println("RESCHDULE");
        registerObserver();
        super.onReschedule(newJobId);
    }

    private void registerObserver() {
        Looper.prepare();

        if (mediaStorageObserver == null) {
            mediaStorageObserver = new MediaStorageObserver(new Handler());
            contentResolver.registerContentObserver(uri, true, mediaStorageObserver);
        }
        Looper.loop();
    }

    private void unregisterObserver() {
        if (contentResolver != null) {
            contentResolver.unregisterContentObserver(mediaStorageObserver);
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


            countCursor = contentResolver.query(uri,
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
                sendDriveBroadCast();
            } else {
                System.out.println("사진 추가는 아님");
            }

            System.out.println(" JOB : 스토리지 갯수 : " + storageCount);
        }
    }

    private void sendDriveBroadCast() {
        Intent intent = new Intent();
        intent.setAction("com.worksmobile.wm_project.NEW_MEDIA");
        intent.setClass(getContext(), MyBroadCastReceiver.class);
        getContext().sendBroadcast(intent);
    }

    public static void scheduleJob() {
        new JobRequest.Builder(MediaStoreJob.TAG)
                .setExecutionWindow(10L, TimeUnit.MINUTES.toMillis(15))
//                .setPeriodic(TimeUnit.MINUTES.toMillis(16))
                .setBackoffCriteria(1000, JobRequest.BackoffPolicy.LINEAR)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}

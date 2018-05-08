package com.worksmobile.wmproject.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import com.worksmobile.wmproject.content_observer.MediaStoreObserver;

public class MediaStoreService extends Service {
    private static final String TAG = "MEDIASTORE_SERVICE";
    private static final Uri IMAGE_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final Uri VIDEO_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private MediaStoreObserver imageObserver;
    private MediaStoreObserver videoObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Start");
        registerObserver();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterObserver();
        Log.d(TAG, "Service Destroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerObserver() {
        if (imageObserver == null && videoObserver == null) {
            imageObserver = new MediaStoreObserver(new Handler(), MediaStoreService.this, IMAGE_CONTENT_URI);
            videoObserver = new MediaStoreObserver(new Handler(), MediaStoreService.this, VIDEO_CONTENT_URI);
            getContentResolver().registerContentObserver(IMAGE_CONTENT_URI, false, imageObserver);
            getContentResolver().registerContentObserver(VIDEO_CONTENT_URI, false, videoObserver);
            Log.d(TAG, "Media Store Service Observer 등록 ");
        }
    }


    private void unregisterObserver() {
        if (getContentResolver() != null) {
            getContentResolver().unregisterContentObserver(imageObserver);
            getContentResolver().unregisterContentObserver(videoObserver);
            imageObserver = null;
            videoObserver = null;
        }
    }
}

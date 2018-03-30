package com.worksmobile.wmproject.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.worksmobile.wmproject.MediaStoreObserver;

public class MediaStoreService extends Service {
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private MediaStoreObserver mediaStorageObserver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerObserver();
        return START_STICKY;
    }

    public MediaStoreService() {
        super();
        System.out.println("Service Start");
    }

    @Override
    public void onDestroy() {
        unregisterObserver();
        System.out.println("Service Destroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerObserver() {
        if (mediaStorageObserver == null) {
            mediaStorageObserver = new MediaStoreObserver(new Handler(), MediaStoreService.this);
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
}

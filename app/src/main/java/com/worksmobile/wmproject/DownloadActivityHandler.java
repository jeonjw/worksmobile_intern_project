package com.worksmobile.wmproject;

import android.os.Handler;
import android.os.Message;

import com.worksmobile.wmproject.ui.DownloadActivity;

import java.lang.ref.WeakReference;

public class DownloadActivityHandler extends Handler {

    private final WeakReference<DownloadActivity> activityWeakReference;

    public DownloadActivityHandler(DownloadActivity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        DownloadActivity activity = activityWeakReference.get();
        activity.handleMessage(msg);
    }
}
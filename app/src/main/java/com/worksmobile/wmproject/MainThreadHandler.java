package com.worksmobile.wmproject;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.worksmobile.wmproject.service.BackgroundDriveService;

import java.lang.ref.WeakReference;

public class MainThreadHandler extends Handler {

    private final WeakReference<BackgroundDriveService> backgroundService;

    public MainThreadHandler(BackgroundDriveService service) {
        backgroundService = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
        BackgroundDriveService service = backgroundService.get();
        service.handleMessage(msg);
    }
}

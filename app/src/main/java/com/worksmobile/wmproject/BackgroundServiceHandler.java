package com.worksmobile.wmproject;


import android.os.Handler;
import android.os.Message;

import com.worksmobile.wmproject.service.BackgroundUploadService;

import java.lang.ref.WeakReference;

public class BackgroundServiceHandler extends Handler {

    private final WeakReference<BackgroundUploadService> backgroundService;

    public BackgroundServiceHandler(BackgroundUploadService service) {
        backgroundService = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
        BackgroundUploadService service = backgroundService.get();
        service.handleMessage(msg);
    }
}

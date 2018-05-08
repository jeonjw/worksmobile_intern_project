package com.worksmobile.wmproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.worksmobile.wmproject.service.BackgroundUploadService;
import com.worksmobile.wmproject.service.MediaStoreService;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BROADCAST_RECEIVER";
    private ConnectivityManager connectivityManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        Log.d(TAG, "ACTION : " + actionName);

        switch (actionName) {
            case "com.worksmobile.wm_project.NEW_MEDIA":
                if (isUnMeteredNetWork(context))
                    startUploadService(context);
                break;
            case "android.intent.action.BOOT_COMPLETED":
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    context.startService(new Intent(context, MediaStoreService.class));
                break;

            case "android.net.conn.CONNECTIVITY_CHANGE":
            case "android.net.conn.CONNECTIVITY_CHANGE_V24":
                if (isUnMeteredNetWork(context))
                    startUploadService(context);
                break;
        }
    }

    private boolean isUnMeteredNetWork(Context context) {
        if (connectivityManager == null)
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnectedOrConnecting()
                && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX);
    }

    private void startUploadService(Context context) {
        Intent serviceIntent = new Intent(context, BackgroundUploadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(serviceIntent);
        else
            context.startService(serviceIntent);
    }
}

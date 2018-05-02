package com.worksmobile.wmproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import com.worksmobile.wmproject.service.BackgroundUploadService;
import com.worksmobile.wmproject.service.MediaStoreService;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private ConnectivityManager connectivityManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        System.out.println("ACTION : " + actionName);
        Toast.makeText(context, "받은 액션 : " + actionName, Toast.LENGTH_SHORT).show();

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
            case "android.net.conn.CONNECTIVITY_CHANGE_V21":
                if (isUnMeteredNetWork(context))
                    startUploadService(context);
                break;
        }
    }

    private boolean isUnMeteredNetWork(Context context) {
        if (connectivityManager == null)
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX)
                return true;
        }
        return false;
    }

    private void startUploadService(Context context) {
        Intent serviceIntent = new Intent(context, BackgroundUploadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(serviceIntent);
        else
            context.startService(serviceIntent);
    }
}

package com.worksmobile.wmproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import com.worksmobile.wmproject.service.BackgroundDriveService;
import com.worksmobile.wmproject.service.MediaStoreService;

public class MyBroadCastReceiver extends BroadcastReceiver {
    private ConnectivityManager manager;

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
        if (manager == null)
            manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX)
                return true;
        }
        return false;
    }

    private void startUploadService(Context context) {
        Intent serviceIntent = new Intent(context, BackgroundDriveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(serviceIntent);
        else
            context.startService(serviceIntent);
    }
}

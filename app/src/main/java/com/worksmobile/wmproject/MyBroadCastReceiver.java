package com.worksmobile.wmproject;

import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.widget.Toast;

import com.worksmobile.wmproject.service.BackgroundDriveService;
import com.worksmobile.wmproject.service.MediaStoreService;

public class MyBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        System.out.println("ACTION : " + actionName);
        Toast.makeText(context, "받은 액션 : " + actionName, Toast.LENGTH_SHORT).show();
        Intent serviceIntent = new Intent(context, BackgroundDriveService.class);

        switch (actionName) {
            case "com.worksmobile.wm_project.NEW_MEDIA":
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                int networkType = manager.getActiveNetworkInfo().getType();

                if (networkType == ConnectivityManager.TYPE_WIFI || networkType == ConnectivityManager.TYPE_WIMAX)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startForegroundService(serviceIntent);
                    else
                        context.startService(serviceIntent);
                break;
            case "android.intent.action.BOOT_COMPLETED":
                context.startService(new Intent(context, MediaStoreService.class));
                break;

            case "android.net.conn.CONNECTIVITY_CHANGE":
                Toast.makeText(context, "받은 액션 : " + actionName, Toast.LENGTH_SHORT).show();
                System.out.println("ConnectivityChange");
                break;

        }
    }
}

package com.worksmobile.wmproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.worksmobile.wmproject.android_job.BackgroundUploadJob;

public class MyBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        Toast.makeText(context, "받은 액션 : " + actionName, Toast.LENGTH_SHORT).show();
        System.out.println("ACTION : " + actionName);

        if (actionName != null && actionName.equals("com.worksmobile.wm_project.NEW_MEDIA")) {

            BackgroundUploadJob.scheduleJob();
//            Intent serviceIntent = new Intent(context, BackgroundDriveService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                context.startForegroundService(serviceIntent);
//            else
//                context.startService(serviceIntent);

        }
    }
}

package com.worksmobile.wmproject.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.worksmobile.wmproject.MyBroadCastReceiver;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectivityJobService extends JobService {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        System.out.println("Connectivity JOBSERVICE 시작");

        sendConnectivityBroadCast();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("Connectivity JOBSERVICE 종료");
        sendConnectivityBroadCast();
        return true;
    }

    private void sendConnectivityBroadCast() {
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE_V24");
        intent.setClass(this, MyBroadCastReceiver.class);
        sendBroadcast(intent);

    }

}

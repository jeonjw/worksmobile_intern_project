package com.worksmobile.wmproject.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.worksmobile.wmproject.MyBroadcastReceiver;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectivityJobService extends JobService {
    private static final int CONNECTIVITY_JOB_ID = 102;
    private static final JobInfo CONNECTIVITY_JOB;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private int firstAvailableCount;

    static {
        CONNECTIVITY_JOB = new JobInfo.Builder(CONNECTIVITY_JOB_ID, new ComponentName("com.worksmobile.wm_project", ConnectivityJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setOverrideDeadline(3000)
                .setPersisted(true)
                .build();
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.i("CONNECTIVITY JOB", "CONNECTIVITY JOB START");
        addConnectivityCallback();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i("CONNECTIVITY JOB", "CONNECTIVITY JOB FINISH");
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            connectivityManager.unregisterNetworkCallback(networkCallback);
        reScheduleJob(jobParameters);
        return false;
    }

    private void addConnectivityCallback() {
        System.out.println("ADD CALLBACK");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                firstAvailableCount++;
                sendConnectivityBroadCast();
            }

            @Override
            public void onLost(Network network) {
                sendConnectivityBroadCast();
            }
        };
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    private void sendConnectivityBroadCast() {
        if (firstAvailableCount <= 2)
            return;

        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE_V24");
        intent.setClass(this, MyBroadcastReceiver.class);
        sendBroadcast(intent);
    }

    public static boolean isScheduled(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == CONNECTIVITY_JOB_ID) {
                return true;
            }
        }
        return false;
    }

    public static void cancelJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.cancel(CONNECTIVITY_JOB_ID);
    }

    private void reScheduleJob(JobParameters jobParameters) {
        if (isScheduled(ConnectivityJobService.this)) {
            cancelJob(ConnectivityJobService.this);
        }
        scheduleJob(ConnectivityJobService.this);
        jobFinished(jobParameters, false);
    }

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(CONNECTIVITY_JOB);
        Log.i("CONNECTIVITY JOB", "CONNECTIVITY JOB SERVICE SCHEDULED");
    }
}

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
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.worksmobile.wmproject.MediaStoreObserver;
import com.worksmobile.wmproject.MyBroadCastReceiver;

import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaStoreJobService extends JobService {

    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private MediaStoreObserver mediaStoreObserver;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private int firstAvailableCount;

    static final JobInfo JOB_MEDIASTORE;

    static {
        JOB_MEDIASTORE = new JobInfo.Builder(101, new ComponentName("com.worksmobile.wm_project", MediaStoreJobService.class.getName()))
                .setBackoffCriteria(100, JobInfo.BACKOFF_POLICY_LINEAR)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setOverrideDeadline(3000)
                .setPersisted(true)
                .build();
    }

    public MediaStoreJobService() {
        super();
        System.out.println("MediaJobService Start");
    }

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(JOB_MEDIASTORE);
        Log.i("MEDIASTORE JOB SERVICE", "MEDIASTORE JOB SERVICE SCHEDULED");

    }

    public static boolean isScheduled(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == 101) {
                return true;
            }
        }
        return false;
    }

    public static void cancelJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.cancel(101);
    }

    private void reScheduleJob(JobParameters jobParameters) {
        if (isScheduled(MediaStoreJobService.this)) {
            cancelJob(MediaStoreJobService.this);
        }
        scheduleJob(MediaStoreJobService.this);
        jobFinished(jobParameters, false);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        registerObserver();
        addConnectivityCallback();
        return true;
    }

    private void addConnectivityCallback() {
        System.out.println("ADD CALLBACK");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                firstAvailableCount++;
                System.out.println("Connection : onAvailable " + firstAvailableCount);
                sendConnectivityBroadCast();
            }

            @Override
            public void onLost(Network network) {
                System.out.println("Connection : onLost");
                sendConnectivityBroadCast();
            }
        };
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("JOB_SERVICE 종료");
        unregisterObserver();
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            connectivityManager.unregisterNetworkCallback(networkCallback);
        reScheduleJob(jobParameters);
        return false;
    }

    private void registerObserver() {
        if (mediaStoreObserver == null) {
            mediaStoreObserver = new MediaStoreObserver(new Handler(), MediaStoreJobService.this);
            getContentResolver().registerContentObserver(uri, true, mediaStoreObserver);
        }
    }

    private void unregisterObserver() {
        if (getContentResolver() != null) {
            getContentResolver().unregisterContentObserver(mediaStoreObserver);
        }
        if (mediaStoreObserver != null) {
            mediaStoreObserver = null;
        }
    }

    private void sendConnectivityBroadCast() {
        if (firstAvailableCount <= 2)
            return;

        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE_V24");
        intent.setClass(this, MyBroadCastReceiver.class);
        sendBroadcast(intent);
    }


}

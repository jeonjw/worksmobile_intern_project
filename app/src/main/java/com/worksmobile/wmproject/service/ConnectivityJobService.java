package com.worksmobile.wmproject.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.worksmobile.wmproject.MyBroadCastReceiver;

import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectivityJobService extends JobService {

    static final JobInfo JOB_UNMETERED;
    static {
        JOB_UNMETERED = new JobInfo.Builder(777, new ComponentName("com.worksmobile.wm_project", ConnectivityJobService.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .build();

    }

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(JOB_UNMETERED);
        Log.i("ConnectivityJobService", "ConnectivityJob SCHEDULED!");

    }

    public static boolean isScheduled(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == 777) {
                return true;
            }
        }
        return false;
    }

    public static void cancelJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        js.cancel(777);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        System.out.println("Connectivity JOBSERVICE 시작");
        sendConnectivityBroadCast();
        reScheduleJob(jobParameters);

        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("Connectivity JOBSERVICE 종료");

        return false;
    }

    private void reScheduleJob(JobParameters jobParameters) {
        if (isScheduled(ConnectivityJobService.this)) {
            cancelJob(ConnectivityJobService.this);
        }
        scheduleJob(ConnectivityJobService.this);
        jobFinished(jobParameters, false);
    }


    private void sendConnectivityBroadCast() {
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE_V24");
        intent.setClass(this, MyBroadCastReceiver.class);
        sendBroadcast(intent);
    }

}

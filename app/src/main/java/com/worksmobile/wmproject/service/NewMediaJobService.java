package com.worksmobile.wmproject.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.worksmobile.wmproject.MyBroadcastReceiver;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.util.FileUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@RequiresApi(api = Build.VERSION_CODES.N)
public class NewMediaJobService extends JobService {

    static final JobInfo NEW_MEDIA_JOB;
    private static final int CALLBACK_PRESENT_INTEGER = 0;
    static final List<String> EXTERNAL_PATH_SEGMENTS = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPathSegments();
    final Handler handler = new Handler();
    private Runnable broakdCastTask;

    static {
        NEW_MEDIA_JOB = new JobInfo.Builder(103, new ComponentName("com.worksmobile.wm_project", NewMediaJobService.class.getName()))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
                .setTriggerContentUpdateDelay(1)
                .setTriggerContentMaxDelay(100)
                .build();
    }

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(NEW_MEDIA_JOB);
        Log.i("NEW_MEDIA_JOB SERVICE", "NEW_MEDIA_JOB SERVICE SCHEDULED");
    }

    private Runnable getBroadCastTask() {
        return new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(CALLBACK_PRESENT_INTEGER);
                Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
                intent.setClass(getApplicationContext(), MyBroadcastReceiver.class);
                getApplicationContext().sendBroadcast(intent);
            }
        };
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i("NewMediaJobService", "JOB STARTED!");
        if (params.getTriggeredContentAuthorities() != null && params.getTriggeredContentUris() != null) {
            for (Uri uri : params.getTriggeredContentUris()) {
                if (!uri.toString().contains("?blocking=1")) {
                    List<String> pathSegments = uri.getPathSegments();
                    if (pathSegments != null && pathSegments.size() == EXTERNAL_PATH_SEGMENTS.size() + 1) {
                        String path = FileUtils.getPath(this, uri);
                        if (isDownlodFileFromDrive(path)) {
                            return false;
                        }
                        System.out.println("Media 추가");
                        addDatabase(path);
                        sendBroadcast();
                    }
                }
            }
        }
        reScheduleJob(params);
        return false;

    }

    private void addDatabase(String path) {
        DateFormat sdFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
        Date nowDate = new Date();
        String tempDate = sdFormat.format(nowDate);
        AppDatabase.getDatabase(this).fileDAO().insertFileStatus(new FileStatus(path, tempDate, "UPLOAD"));
    }

    private void sendBroadcast() {
        if (handler.hasMessages(CALLBACK_PRESENT_INTEGER) && broakdCastTask != null) {
            handler.removeCallbacks(broakdCastTask);
        }
        broakdCastTask = getBroadCastTask();
        handler.postDelayed(broakdCastTask, 3000);
    }

    private boolean isDownlodFileFromDrive(String location) {
        return location.contains("/storage/emulated/0/DCIM/WorksDrive/");
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    public static boolean isScheduled(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == 103) {
                return true;
            }
        }
        return false;
    }

    public static void cancelJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.cancel(103);
    }

    private void reScheduleJob(JobParameters jobParameters) {
        if (isScheduled(NewMediaJobService.this)) {
            cancelJob(NewMediaJobService.this);
        }
        scheduleJob(NewMediaJobService.this);
        jobFinished(jobParameters, false);
    }


}

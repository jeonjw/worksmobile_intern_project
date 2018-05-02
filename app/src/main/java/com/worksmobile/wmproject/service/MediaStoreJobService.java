package com.worksmobile.wmproject.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.worksmobile.wmproject.content_observer.MediaStoreObserver;

import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaStoreJobService extends JobService {

    private static final int MEDIASTORE_JOB_ID = 101;
    private static final Uri IMAGE_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final Uri VIDEO_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private MediaStoreObserver imageObserver;
    private MediaStoreObserver videoObserver;

    static final JobInfo MEDIASTORE_JOB;

    static {
        MEDIASTORE_JOB = new JobInfo.Builder(MEDIASTORE_JOB_ID, new ComponentName("com.worksmobile.wm_project", MediaStoreJobService.class.getName()))
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
        jobScheduler.schedule(MEDIASTORE_JOB);
        Log.i("MEDIASTORE JOB SERVICE", "MEDIASTORE JOB SERVICE SCHEDULED");

    }

    public static boolean isScheduled(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        List<JobInfo> jobs = js.getAllPendingJobs();
        if (jobs == null) {
            return false;
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId() == MEDIASTORE_JOB_ID) {
                return true;
            }
        }
        return false;
    }

    public static void cancelJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        js.cancel(MEDIASTORE_JOB_ID);
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
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("JOB_SERVICE 종료");
        unregisterObserver();
        reScheduleJob(jobParameters);
        return false;
    }

    private void registerObserver() {
        imageObserver = new MediaStoreObserver(new Handler(), MediaStoreJobService.this, IMAGE_CONTENT_URI);
        videoObserver = new MediaStoreObserver(new Handler(), MediaStoreJobService.this, VIDEO_CONTENT_URI);
        getContentResolver().registerContentObserver(IMAGE_CONTENT_URI, true, imageObserver);
        getContentResolver().registerContentObserver(VIDEO_CONTENT_URI, true, videoObserver);

    }

    private void unregisterObserver() {
        if (getContentResolver() != null) {
            getContentResolver().unregisterContentObserver(imageObserver);
            getContentResolver().unregisterContentObserver(videoObserver);
        }
    }


}

package com.worksmobile.wmproject.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;

import com.worksmobile.wmproject.MediaStoreObserver;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MediaStoreJobService extends JobService {

    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    private MediaStoreObserver mediaStoreObserver;

    public MediaStoreJobService() {
        super();
        System.out.println("JobService Start");
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        registerObserver();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        System.out.println("JOB_SERVICE 종료");
        jobFinished(jobParameters, true);
        unregisterObserver();
        return true;
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
}

package com.worksmobile.wmproject.android_job;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class MediaJobCreator  implements JobCreator {

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case MediaStoreJob.TAG:
                return new MediaStoreJob();
            case BackgroundUploadJob.TAG:
                return new BackgroundUploadJob();
            default:
                return null;
        }
    }
}
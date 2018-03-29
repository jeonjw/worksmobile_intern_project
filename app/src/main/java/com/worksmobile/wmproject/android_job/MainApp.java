package com.worksmobile.wmproject.android_job;

import android.app.Application;

import com.evernote.android.job.JobManager;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new MediaJobCreator());
    }
}

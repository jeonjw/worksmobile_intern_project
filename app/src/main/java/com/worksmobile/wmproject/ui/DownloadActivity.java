package com.worksmobile.wmproject.ui;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.DownloadActivityHandler;
import com.worksmobile.wmproject.DownloadRecyclerViewAdapter;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.io.File;
import java.util.ArrayList;

public class DownloadActivity extends AppCompatActivity {

    public static final int READY = 300;
    public static final int DOWNLOAD_REQUEST = 301;

    private DownloadRecyclerViewAdapter adapter;
    private Handler mainThreadHandler;
    private DownloadHandlerThread handlerThread;

    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private ArrayList<DriveFile> fileList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);


        dbHelper = new DBHelpler(this);
        driveHelper = new DriveHelper(this);

        fileList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("DOWNLOAD_LIST");

        Toolbar toolbar = findViewById(R.id.download_activity_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        RecyclerView recyclerView = findViewById(R.id.download_recyclerview);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadRecyclerViewAdapter(fileList);
        recyclerView.setAdapter(adapter);

        mainThreadHandler = new DownloadActivityHandler(this);
        handlerThread = new DownloadHandlerThread("DownloadHandlerThread");
        handlerThread.start();
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case READY:
                createDownloadRequest();
                break;

        }
    }

    private void createDownloadRequest() {
        for (DriveFile file : fileList) {
            handlerThread.sendDownloadRequest(file);
        }
    }

    class DownloadHandlerThread extends HandlerThread {

        private Handler handler;

        public DownloadHandlerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case DOWNLOAD_REQUEST:
                            DriveFile file = (DriveFile) msg.obj;
                            executeDownload(file.getId(), file.getName());
                            break;

                    }
                }

            };
            Message message = handler.obtainMessage(READY);
            mainThreadHandler.sendMessageAtFrontOfQueue(message);
        }

        public void sendDownloadRequest(DriveFile file) {
            Message message = handler.obtainMessage(DOWNLOAD_REQUEST, file);
            handler.sendMessage(message);
        }

        private void executeDownload(String fileId, String fileName) {
            File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "test.jpeg");

            driveHelper.downloadFile(fileId, downloadedFile, new StateCallback() {
                @Override
                public void onSuccess(String msg) {
                    System.out.println("DOWNLOAD : " + msg);
                }

                @Override
                public void onFailure(String msg) {

                }
            });

        }

    }
}

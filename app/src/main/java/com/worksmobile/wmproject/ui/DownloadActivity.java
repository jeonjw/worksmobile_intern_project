package com.worksmobile.wmproject.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.MenuItem;

import com.worksmobile.wmproject.ContractDB;
import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.DownloadActivityHandler;
import com.worksmobile.wmproject.DownloadRecyclerViewAdapter;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.retrofit_object.DownloadItem;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadActivity extends AppCompatActivity {

    public static final int READY = 300;
    public static final int DOWNLOAD_REQUEST = 301;
    public static final int PROGRESS_UPDATE = 302;

    private DownloadRecyclerViewAdapter adapter;
    private Handler mainThreadHandler;
    private DownloadHandlerThread handlerThread;

    private DriveHelper driveHelper;
    private DBHelpler dbHelper;
    private ArrayList<DriveFile> downloadRequestList;
    private ArrayList<DownloadItem> downloadFinishItemList;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        dbHelper = new DBHelpler(this);
        driveHelper = new DriveHelper(this);
        downloadFinishItemList = new ArrayList<>();

        downloadRequestList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("DOWNLOAD_LIST");

        createDownloadList();
        DateFormat sdFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);

        for (DriveFile file : downloadRequestList) {
            downloadFinishItemList.add(0, new DownloadItem(file.getName(), sdFormat.format(new Date()), file.getThumbnailLink(), 0, file.getWidth(), file.getHeight()));
        }

        Toolbar toolbar = findViewById(R.id.download_activity_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        RecyclerView recyclerView = findViewById(R.id.download_recyclerview);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadRecyclerViewAdapter(downloadFinishItemList);
        recyclerView.setAdapter(adapter);

        mainThreadHandler = new DownloadActivityHandler(this);
        handlerThread = new DownloadHandlerThread("DownloadHandlerThread");
        handlerThread.start();
    }

    private void createDownloadList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor downloadCursor = db.rawQuery(ContractDB.SQL_SELECT_DOWNLOAD, null);

        if (downloadCursor.moveToFirst()) {
            do {
                String path = downloadCursor.getString(1);
                String date = downloadCursor.getString(2);

                String fileName = path.substring(path.lastIndexOf("/") + 1);

                downloadFinishItemList.add(0, new DownloadItem(fileName, date, path, 100, 0, 0));
            } while (downloadCursor.moveToNext());

        }
        downloadCursor.close();
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case READY:
                createDownloadRequest();
                break;
            case PROGRESS_UPDATE:
                int position = (int) msg.obj;
                adapter.progressUpdate(position, msg.arg1);
                break;
        }
    }

    private void createDownloadRequest() {
        for (DriveFile file : downloadRequestList) {
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
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case DOWNLOAD_REQUEST:
                            DriveFile file = (DriveFile) msg.obj;
                            executeDownload(file);
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

        private void executeDownload(DriveFile file) {
            int updateItemPosition = downloadRequestList.indexOf(file);
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/WorksDrive";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File downloadedFile = new File(path + "/" + file.getName());

            if (downloadedFile.exists()) {
                System.out.println("다운받을 파일 존재" + FileUtils.getFileNameWithoutExtension(downloadedFile.getName()));
                System.out.println(FileUtils.getDuplicatedFileName(downloadedFile));
            }

            Call<ResponseBody> call = driveHelper.createDownloadCall(file.getId());
            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful()) {

                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), downloadedFile, updateItemPosition);
                    if (writtenToDisk) {
                        dbHelper.insertDB(downloadedFile.getAbsolutePath(), "DOWNLOAD");
                        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        dm.addCompletedDownload(downloadedFile.getName(), "WM_Project_Google_Drive", true, "image/jpeg", downloadedFile.getAbsolutePath(), downloadedFile.length(), false);
                        System.out.println("download HASH : " + downloadedFile.hashCode());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean writeResponseBodyToDisk(ResponseBody body, File destinationFile, int updateItemPosition) {
            try (InputStream inputStream = new BufferedInputStream(body.byteStream(), 4096);
                 OutputStream outputStream = new FileOutputStream(destinationFile)) {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                int readBuffer;
                long fileSizeDownloadedInByte = 0;
                int fileDownloadedInPercentage = 0;

                while (true) {
                    readBuffer = inputStream.read(fileReader);
                    if (readBuffer == -1) {
                        break;
                    }
                    fileSizeDownloadedInByte += readBuffer;
                    fileDownloadedInPercentage = (int) ((fileSizeDownloadedInByte * 100) / fileSize);

                    Message message = mainThreadHandler.obtainMessage(PROGRESS_UPDATE, updateItemPosition);
                    message.arg1 = fileDownloadedInPercentage;
                    mainThreadHandler.sendMessage(message);

                    outputStream.write(fileReader, 0, readBuffer);
                }

                outputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;

            }
        }
    }
}

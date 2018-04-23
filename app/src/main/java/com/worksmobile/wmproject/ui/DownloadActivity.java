package com.worksmobile.wmproject.ui;

import android.app.DownloadManager;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;

import com.worksmobile.wmproject.DownloadActivityHandler;
import com.worksmobile.wmproject.DownloadRecyclerViewAdapter;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.retrofit_object.DownloadItem;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadActivity extends AppCompatActivity {

    public static final int READY = 300;
    public static final int DOWNLOAD_REQUEST = 301;
    public static final int PROGRESS_UPDATE = 302;
    public static final int QUERY = 303;
    public static final int QUERY_FINISH = 304;

    private DownloadRecyclerViewAdapter adapter;
    private Handler mainThreadHandler;
    private DownloadHandlerThread handlerThread;

    private DriveHelper driveHelper;
    private ArrayList<DriveFile> downloadRequestList;
    private ArrayList<DownloadItem> downloadItemList;
    private List<FileStatus> downloadFileListFromDB;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.toolbar_delete_download_list) {
            deleteDownloadList();
        }
        return true;
    }


    private void deleteDownloadList() {
        for (FileStatus fileStatus : downloadFileListFromDB)
            AppDatabase.getDatabase(getBaseContext()).fileDAO().deleteFileStatus(fileStatus);
        downloadItemList.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        driveHelper = new DriveHelper(this);
        downloadItemList = new ArrayList<>();

        downloadRequestList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("DOWNLOAD_LIST");

        Toolbar toolbar = findViewById(R.id.download_activity_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        RecyclerView recyclerView = findViewById(R.id.download_recyclerview);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DownloadRecyclerViewAdapter(downloadItemList);
        recyclerView.setAdapter(adapter);

        mainThreadHandler = new DownloadActivityHandler(this);
        handlerThread = new DownloadHandlerThread("DownloadHandlerThread");
        handlerThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download_activity_toolbar_menu, menu);
        return true;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case READY:
                handlerThread.sendQueryRequest();
                break;
            case QUERY_FINISH:
                DateFormat dateFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
                for (DriveFile file : downloadRequestList) {
                    downloadItemList.add(0, new DownloadItem(file.getName(), dateFormat.format(new Date()), file.getThumbnailLink(), 0, file.getWidth(), file.getHeight()));
                }
                adapter.notifyDataSetChanged();
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
                        case QUERY:
                            createDownloadList();
                            break;
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
                downloadedFile = new File(path + "/" + FileUtils.getDuplicatedFileName(downloadedFile));
            }

            Call<ResponseBody> call = driveHelper.createDownloadCall(file.getId());
            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful()) {

                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), downloadedFile, updateItemPosition);
                    if (writtenToDisk) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
                        Date nowDate = new Date();
                        String tempDate = dateFormat.format(nowDate);
                        AppDatabase.getDatabase(DownloadActivity.this).fileDAO().insertFileStatus(new FileStatus(downloadedFile.getAbsolutePath(), tempDate, "DOWNLOAD"));

                        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        dm.addCompletedDownload(downloadedFile.getName(), "WM_Project_Google_Drive", true, "image/jpeg", downloadedFile.getAbsolutePath(), downloadedFile.length(), false);
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
                int fileDownloadedInPercentage;

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

        public void sendQueryRequest() {
            Message message = handler.obtainMessage(QUERY);
            handler.sendMessageAtFrontOfQueue(message);
        }

        private void createDownloadList() {
            downloadFileListFromDB = AppDatabase.getDatabase(getBaseContext()).fileDAO().getDownloadFileList();
            for (FileStatus fileStatus : downloadFileListFromDB) {
                String path = fileStatus.getLocation();
                String date = fileStatus.getDate();

                String fileName = path.substring(path.lastIndexOf("/") + 1);

                downloadItemList.add(0, new DownloadItem(fileName, date, path, 100, 0, 0));
            }
            mainThreadHandler.sendEmptyMessage(QUERY_FINISH);
        }

    }
}

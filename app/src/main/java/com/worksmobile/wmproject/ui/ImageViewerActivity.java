package com.worksmobile.wmproject.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ViewerPagerAdapter;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.util.FileUtils;
import com.worksmobile.wmproject.value_object.DriveFile;

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
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageViewerActivity extends AppCompatActivity {
    private DriveFile currentFile;
    private DriveHelper driveHelper;
    private ViewerPagerAdapter viewerPageAdapter;
    private ArrayList<DriveFile> fileList;
    private ViewPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        fileList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("FILE_LIST");
        int viewerPosition = getIntent().getIntExtra("VIEWER_POSITION", 0);

        currentFile = fileList.get(viewerPosition);
        driveHelper = new DriveHelper(this);

        Toolbar toolbar = findViewById(R.id.image_viewer_toolbar);
        TextView toolbarTitleTextView = toolbar.findViewById(R.id.viewer_toolbar_text_view);
        toolbarTitleTextView.setText(currentFile.getName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationIcon(R.drawable.ic_close);


        viewerPageAdapter = new ViewerPagerAdapter(this, fileList);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(viewerPageAdapter);
        viewPager.setCurrentItem(viewerPosition);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentFile = fileList.get(position);
                toolbarTitleTextView.setText(currentFile.getName());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.toolbar_detail_file_info) {
            Fragment fragment = new FileDetailInfoFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable("DRIVE_FILE", currentFile);
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction().add(R.id.image_viewer_root_view, fragment).addToBackStack(null).commit();
        } else if (item.getItemId() == R.id.toolbar_download) {
            downloadFile(currentFile);
        } else if (item.getItemId() == R.id.toolbar_delete) {
            deleteFile(currentFile);
        }
        return true;
    }

    private void deleteFile(DriveFile file) {
        driveHelper.enqueueDeleteCall(file, new StateCallback() {
            @Override
            public void onSuccess(String msg) {
                if (msg == null) {
                    int position = viewPager.getCurrentItem();
                    fileList.remove(position);
                    viewerPageAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(String msg) {
            }
        });
    }


    private void downloadFile(DriveFile file) {
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
        File finalDownloadedFile = downloadedFile;
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), finalDownloadedFile);
                    if (writtenToDisk) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
                        Date nowDate = new Date();
                        String tempDate = dateFormat.format(nowDate);
                        AppDatabase.getDatabase(getApplicationContext()).fileDAO().insertFileStatus(new FileStatus(finalDownloadedFile.getAbsolutePath(), tempDate, "DOWNLOAD"));

                        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        dm.addCompletedDownload(finalDownloadedFile.getName(), "WM_Project_Google_Drive", true, FileUtils.getMimeType(finalDownloadedFile), finalDownloadedFile.getAbsolutePath(), finalDownloadedFile.length(), true);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {

            }
        });

    }

    private boolean writeResponseBodyToDisk(ResponseBody body, File destinationFile) {
        try (InputStream inputStream = new BufferedInputStream(body.byteStream(), 4096);
             OutputStream outputStream = new FileOutputStream(destinationFile)) {
            byte[] fileReader = new byte[4096];
            int readBuffer;
            while (true) {
                readBuffer = inputStream.read(fileReader);
                if (readBuffer == -1) {
                    break;
                }
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

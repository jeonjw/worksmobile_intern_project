package com.worksmobile.wmproject.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.widget.VideoView;

import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;

import java.io.File;

public class VideoViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);
        VideoView videoView = findViewById(R.id.video_view);
        String fileName = getIntent().getStringExtra("FILE_NAME");

        String path = getDownlodedFilePath(fileName);
        File file = null;
        if (path != null)
            file = new File(path);
        if (file != null && file.exists()) {
            videoView.setVideoPath(path);
            videoView.start();
        } else {
            Toast.makeText(this, "파일을 다운받으셔야 합니다. " + fileName, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getDownlodedFilePath(String fileName) {
        String path = null;
        for(FileStatus fileStatus :  AppDatabase.getDatabase(this).fileDAO().findPath(fileName)){
            File file = new File(fileStatus.getLocation());
            if (file.exists()) {
                path = fileStatus.getLocation();
                break;
            }
        }
        return path;
    }

}

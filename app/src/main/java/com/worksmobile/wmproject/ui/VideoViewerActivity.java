package com.worksmobile.wmproject.ui;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import android.widget.VideoView;

import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.R;

import java.io.File;

public class VideoViewerActivity extends AppCompatActivity {
    private DBHelpler dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);
        dbHelper = new DBHelpler(this);
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
            Toast.makeText(this, "파일을 다운받으셔야 합니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getDownlodedFilePath(String fileName) {
        String path = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String SQL_SELECT_DOWNLOAD = "SELECT LOCATION, STATUS FROM UPLOAD_TABLE WHERE (STATUS='DOWNLOAD' OR STATUS='UPLOAD') AND LOCATION LIKE " + "'%" + fileName + "%'";

        Cursor downloadCursor = db.rawQuery(SQL_SELECT_DOWNLOAD, null);
        System.out.println("COUNT : " + downloadCursor.getCount());
        if (downloadCursor.moveToFirst()) {
            path = downloadCursor.getString(0);
        }

        downloadCursor.close();
        return path;

    }

}

package com.worksmobile.wmproject.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ViewerPagerAdapter;
import com.worksmobile.wmproject.value_object.DriveFile;

import java.util.ArrayList;

public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ArrayList<DriveFile> fileList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("FILE_LIST");
        int viewerPosition = getIntent().getIntExtra("VIEWER_POSITION", 0);


        ViewerPagerAdapter viewerPageAdapter = new ViewerPagerAdapter(this, fileList);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(viewerPageAdapter);
        viewPager.setCurrentItem(viewerPosition);

    }
}

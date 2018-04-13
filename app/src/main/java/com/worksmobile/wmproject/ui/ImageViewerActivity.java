package com.worksmobile.wmproject.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ViewerPagerAdapter;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {

    private ViewPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        List<String> tempList = new ArrayList<>();

        tempList.add("https://www.ikea.com/kr/ko/images/products/henriksdal-uija-olenji__0462850_PE608355_S4.JPG");
        tempList.add("https://cdn.pixabay.com/photo/2016/02/02/14/42/bank-1175430_960_720.png");
        tempList.add("https://www.ikea.com/kr/ko/images/products/henriksdal-uija-olenji__0462850_PE608355_S4.JPG");
        tempList.add("https://cdn.pixabay.com/photo/2016/02/02/14/42/bank-1175430_960_720.png");
        tempList.add("https://www.ikea.com/kr/ko/images/products/henriksdal-uija-olenji__0462850_PE608355_S4.JPG");
        tempList.add("https://cdn.pixabay.com/photo/2016/02/02/14/42/bank-1175430_960_720.png");

        ArrayList<DriveFile> fileList = (ArrayList<DriveFile>) getIntent().getSerializableExtra("FILE_LIST");
        int viewerPosition = getIntent().getIntExtra("VIEWER_POSITION", 0);


        ViewerPagerAdapter viewerPageAdapter = new ViewerPagerAdapter(this, fileList);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(viewerPageAdapter);
        viewPager.setCurrentItem(viewerPosition);

    }
}

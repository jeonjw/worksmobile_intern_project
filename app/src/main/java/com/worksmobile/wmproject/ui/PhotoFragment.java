package com.worksmobile.wmproject.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ThumbnailItemDecoration;
import com.worksmobile.wmproject.ThumbnailRecyclerViewAdapter;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.OnItemSelectedListener;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.ArrayList;

public class PhotoFragment extends Fragment {


    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeContainer;
    private DriveHelper driveHelper;
    private ArrayList<DriveFile> fileList;
    private ThumbnailRecyclerViewAdapter adapter;

    private View.OnClickListener onClickListener;
    private OnItemSelectedListener onItemSelectedListener;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        driveHelper = new DriveHelper(getContext());
        fileList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.thumbnail_recyclerview);
        initClickListener();

        adapter = new ThumbnailRecyclerViewAdapter(fileList, onClickListener);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.addItemDecoration(new ThumbnailItemDecoration(3, 3));
        recyclerView.setAdapter(adapter);

        swipeContainer = view.findViewById(R.id.pull_to_refresh);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getDriveFIleList();
            }
        });

        getDriveFIleList();

        return view;
    }

    private void initClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                intent.putExtra("FILE_LIST", fileList);
                intent.putExtra("VIEWER_POSITION", itemPosition);
                startActivity(intent);
            }
        };
        onItemSelectedListener = new OnItemSelectedListener() {
            @Override
            public void onSelectChanged(boolean checked, String fileId) {

            }
        };
    }

    private void getDriveFIleList() {
        driveHelper.listFiles("root", new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                fileList.clear();
                for (DriveFile file : driveFiles) {
                    String thumbnailLink = file.getThumbnailLink();
                    file.setThumbnailLink(replaceThumbnailSize(thumbnailLink, "s220", "s550"));
                    fileList.add(file);
                }
                adapter.notifyDataSetChanged();
                if (swipeContainer.isRefreshing())
                    swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    public String replaceThumbnailSize(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }
}

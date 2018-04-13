package com.worksmobile.wmproject.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ThumbnailItemDecoration;
import com.worksmobile.wmproject.ThumbnailRecyclerViewAdapter;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.retrofit_object.Token;

import java.util.ArrayList;
import java.util.List;

public class PhotoFragment extends Fragment {

    private SwipeRefreshLayout swipeContainer;
    private DriveHelper driveHelper;
    private List<String> thumbnailLinkList;
    private ThumbnailRecyclerViewAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);

        thumbnailLinkList = new ArrayList<>();

        RecyclerView recyclerView = view.findViewById(R.id.thumbnail_recyclerview);
        adapter = new ThumbnailRecyclerViewAdapter(thumbnailLinkList);
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


        Token token = restoreAuthState();
        driveHelper = new DriveHelper(getContext().getString(R.string.client_id), null, getContext());
        driveHelper.setToken(token);

        getDriveFIleList();

        return view;
    }

    private void getDriveFIleList() {
        driveHelper.listFiles("root", new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                thumbnailLinkList.clear();
                for (DriveFile file : driveFiles) {
                    String thumbnailLink = file.getThumbnailLink();
                    thumbnailLink = replaceThumbnailSize(thumbnailLink,"s220","s550");
                    thumbnailLinkList.add(thumbnailLink);
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

    public static String replaceThumbnailSize(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }

    @Nullable
    private Token restoreAuthState() {
        Gson gson = new Gson();
        String jsonString = getContext().getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE)
                .getString("TOKEN_STATE", null);


        return gson.fromJson(jsonString, Token.class);
    }
}

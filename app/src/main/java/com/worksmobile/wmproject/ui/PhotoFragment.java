package com.worksmobile.wmproject.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.retrofit_object.Token;

public class PhotoFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);


        Token token = restoreAuthState();
        DriveHelper driveHelper = new DriveHelper(getContext().getString(R.string.client_id), null, getContext());
        driveHelper.setToken(token);
        driveHelper.listFiles("root", new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                System.out.println("FILES");
                System.out.println(driveFiles.length);
                for (DriveFile file : driveFiles) {
                    System.out.println("FILE : " + file.toString());
                    driveHelper.getFile(file.getId(), null);
                }
            }

            @Override
            public void onFailure(String msg) {

            }
        });
        return view;
    }

    @Nullable
    private Token restoreAuthState() {
        Gson gson = new Gson();
        String jsonString = getContext().getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE)
                .getString("TOKEN_STATE", null);


        return gson.fromJson(jsonString, Token.class);
    }
}

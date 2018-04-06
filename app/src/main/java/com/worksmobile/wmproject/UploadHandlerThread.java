package com.worksmobile.wmproject;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class UploadHandlerThread extends HandlerThread {

    private static final int UPLOAD = 700;
    private Handler handler;

    public UploadHandlerThread(String name) {
        super(name);
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPLOAD:
                        Call<UploadResult> call = (Call<UploadResult>) msg.obj;
                        try {
                            Response<UploadResult> response = call.execute();
                            String message = DriveUtils.printResponse("uploadFile", response);
                            if (message == null) { //성공

                            } else {

                            } //실패
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };
    }

    public void executeUpload(Call<UploadResult> call) {
        Message message = handler.obtainMessage(UPLOAD, call);
        handler.sendMessage(message);

    }


}

package com.worksmobile.wmproject.callback;

public interface UploadCallback {
    void onSuccess(String databaseID);

    void onFailure(String msg,String databaseID);
}

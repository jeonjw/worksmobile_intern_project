package com.worksmobile.wmproject.callback;

import com.worksmobile.wmproject.value_object.DriveFile;

public interface ListCallback {
    void onSuccess(DriveFile[] driveFiles);
    void onFailure(String msg);
}

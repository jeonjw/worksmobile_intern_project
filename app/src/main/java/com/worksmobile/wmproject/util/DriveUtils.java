package com.worksmobile.wmproject.util;

import android.util.Log;

import java.io.IOException;

import retrofit2.Response;

public final class DriveUtils {

    private static final String SUCCESS = "SUCCESS";
    private static final String TAG = "DRIVE_UTIL";

    private DriveUtils() {
    }

    public static String printResponse(String msg, Response<?> response) {
        if (response.isSuccessful()) {
            Log.e(TAG, "success " + response.body());
            return SUCCESS;
        } else {
            String errorMessage = null;
            try {
                errorMessage = response.errorBody().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.w(TAG, "failure " + errorMessage);
            return msg + " failed: " + errorMessage;
        }
    }

    public static String printFailure(String msg, Throwable t) {
        String message = Log.getStackTraceString(t);
        Log.w(TAG, "failure " + message);
        return msg + " failed: " + message;
    }
}
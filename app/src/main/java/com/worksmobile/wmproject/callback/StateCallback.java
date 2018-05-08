package com.worksmobile.wmproject.callback;

public interface StateCallback {
    void onSuccess(String msg);

    default void onFailure(String msg) {}
}
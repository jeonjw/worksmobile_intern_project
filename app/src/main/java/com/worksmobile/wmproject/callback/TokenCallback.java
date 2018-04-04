package com.worksmobile.wmproject.callback;


import com.worksmobile.wmproject.retrofit_object.Token;

public interface TokenCallback {
    void onSuccess(Token token);

    void onFailure(String msg);
}

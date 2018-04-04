package com.worksmobile.wmproject;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DriveHelper {

    private static final String BASE_URL_API = "https://www.googleapis.com";
    private static final String BASE_URL_ACCOUNT = "https://accounts.google.com";
    public static final String REDIRECT_URI = "com.worksmobile.wmproject:/oauth2callback";

    public String clientId;
    public String clientSecret;
    private Token token;


    private DriveApi mDriveApi;

    public DriveHelper(String clientId, String clientSecret) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mDriveApi = retrofit.create(DriveApi.class);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void getToken(final TokenCallback callback, String mAuthCode) {

        Call<Token> call = mDriveApi.getToken(mAuthCode, clientId,
                clientSecret, REDIRECT_URI, "authorization_code");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("getToken", response);
                if (message == null) {
                    token = response.body();
                    token.setTokenTimestamp(System.currentTimeMillis());

                    if (callback != null) {
                        callback.onSuccess(token);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("getToken", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void refreshToken(final TokenCallback callback) {
        checkRefreshToken();
        Call<Token> call = mDriveApi.refreshToken(token.getRefreshToken(), clientId, clientSecret, "refresh_token");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("refreshToken", response);
                if (message == null) {
                    Token refreshTtoken = response.body();
                    token.setAccessToken(refreshTtoken.getAccessToken());
                    if (callback != null) {
                        callback.onSuccess(token);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("refreshToken", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }


    public void uploadFile(File srcFile, final StateCallback callback) {
        if (!srcFile.exists())
            return;

        MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
        String content = "{\"name\": \"" + srcFile.getName() + "\"}";
        MultipartBody.Part metaPart = MultipartBody.Part.create(RequestBody.create(contentType, content));
        String mimeType = getMimeType(srcFile);
        MultipartBody.Part dataPart = MultipartBody.Part.create(RequestBody.create(MediaType.parse(mimeType), srcFile));

        Call<UploadResult> call = mDriveApi.uploadFile(getAuthToken(), metaPart, dataPart);
        call.enqueue(new Callback<UploadResult>() {
            @Override
            public void onResponse(@NonNull Call<UploadResult> call, @NonNull Response<UploadResult> response) {
                String message = DriveUtils.printResponse("uploadFile", response);
                if (message == null) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResult> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("uploadFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    private String getAuthToken() {
        checkAccessToken();
        return String.format("Bearer %s", token.getAccessToken());
    }

    private void checkRefreshToken() {
        if (token.getRefreshToken() == null) {
            throw new IllegalStateException("Refresh token is null!");
        }
    }

    private void checkAccessToken() {
        if (token.getAccessToken() == null) {
            throw new IllegalStateException("Access token is null!");
        }
    }


    public String getExtension(File file) {
        return getExtension(file.getName());
    }

    public String getExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (!lowerName.contains("."))
            return "";
        return lowerName.substring(lowerName.lastIndexOf(".") + 1);
    }

    public String getMimeType(File file) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String mimeType = map.getMimeTypeFromExtension(getExtension(file));
        if (TextUtils.isEmpty(mimeType))
            return "*/*";
        return mimeType;
    }
}

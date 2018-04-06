package com.worksmobile.wmproject;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.callback.UploadCallback;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DriveHelper {

    private static final String SUCCESS = "SUCCESS";
    private static final String BASE_URL_API = "https://www.googleapis.com";
    private static final String BASE_URL_ACCOUNT = "https://accounts.google.com";
    public static final String REDIRECT_URI = "com.worksmobile.wmproject:/oauth2callback";

    public String clientId;
    public String clientSecret;
    private Token token;
    private DriveApi driveApi;

    public DriveHelper(String clientId, String clientSecret) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        driveApi = retrofit.create(DriveApi.class);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void getToken(final TokenCallback callback, String mAuthCode) {

        Call<Token> call = driveApi.getToken(mAuthCode, clientId,
                clientSecret, REDIRECT_URI, "authorization_code");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("getToken", response);
                if (message == SUCCESS) {
                    token = response.body();
                    token.setTokenTimeStamp(System.currentTimeMillis());

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
        Call<Token> call = driveApi.refreshToken(token.getRefreshToken(), clientId, clientSecret, "refresh_token");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("refreshToken", response);
                if (message == SUCCESS) {
                    Token refreshTtoken = response.body();
                    token.setAccessToken(refreshTtoken.getAccessToken());
                    token.setTokenTimeStamp(System.currentTimeMillis());
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

    public void uploadFile(File srcFile, String databaseID, final UploadCallback callback) {
        if (!srcFile.exists())
            return;

        MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
        String content = "{\"name\": \"" + srcFile.getName() + "\"}";
        MultipartBody.Part metaPart = MultipartBody.Part.create(RequestBody.create(contentType, content));
        String mimeType = getMimeType(srcFile);
        MultipartBody.Part dataPart = MultipartBody.Part.create(RequestBody.create(MediaType.parse(mimeType), srcFile));

        Call<UploadResult> call = driveApi.uploadFile(getAuthToken(), metaPart, dataPart);
        call.enqueue(new Callback<UploadResult>() {
            @Override
            public void onResponse(@NonNull Call<UploadResult> call, @NonNull Response<UploadResult> response) {
                String message = DriveUtils.printResponse("uploadFile", response);
                if (message == SUCCESS) {
                    if (callback != null) {
                        callback.onSuccess(databaseID);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message, databaseID);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResult> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("uploadFile", t);
                if (callback != null) {
                    callback.onFailure(message, databaseID);
                }
            }
        });
    }

    public Call<UploadResult> createUploadCall(String imageLocation) {
        File srcFile = new File(imageLocation);
        if (!srcFile.exists())
            return null;

        MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
        String content = "{\"name\": \"" + srcFile.getName() + "\"}";
        MultipartBody.Part metaPart = MultipartBody.Part.create(RequestBody.create(contentType, content));
        String mimeType = getMimeType(srcFile);
        MultipartBody.Part dataPart = MultipartBody.Part.create(RequestBody.create(MediaType.parse(mimeType), srcFile));

        return driveApi.uploadFile(getAuthToken(), metaPart, dataPart);
    }

    public void uploadFileSync(Cursor uploadCursor, final UploadCallback callback) {
        List<Call<UploadResult>> uploadCallList = new ArrayList<>();
        List<String> databaseIDList = new ArrayList<>();
        if (uploadCursor.moveToFirst()) {
            do {
                String databaseID = uploadCursor.getString(0);
                String imageLocation = uploadCursor.getString(1);
                File srcFile = new File(imageLocation);

                if (!srcFile.exists())
                    continue;

                MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
                String content = "{\"name\": \"" + srcFile.getName() + "\"}";
                MultipartBody.Part metaPart = MultipartBody.Part.create(RequestBody.create(contentType, content));
                String mimeType = getMimeType(srcFile);
                MultipartBody.Part dataPart = MultipartBody.Part.create(RequestBody.create(MediaType.parse(mimeType), srcFile));

                Call<UploadResult> call = driveApi.uploadFile(getAuthToken(), metaPart, dataPart);
                uploadCallList.add(call);
                databaseIDList.add(databaseID);
                System.out.println("요청 전");
            } while (uploadCursor.moveToNext());
        }

        new Thread(() -> {
            for (int i = 0; i < uploadCallList.size(); i++) {
                Call<UploadResult> call = uploadCallList.get(i);
                String databaseID = databaseIDList.get(i);

                try {
                    System.out.println("요청1");
                    Response<UploadResult> response = call.execute();
                    System.out.println("요청2");
                    String message = DriveUtils.printResponse("uploadFile", response);
                    if (message == SUCCESS) {
                        if (callback != null)
                            callback.onSuccess(databaseID);
                    } else {
                        if (callback != null)
                            callback.onFailure(message, databaseID);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (callback != null)
                        callback.onFailure(null, databaseID);
                }
            }
        }).start();
    }

    public String getAuthToken() {
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

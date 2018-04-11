package com.worksmobile.wmproject;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

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

import static com.worksmobile.wmproject.service.BackgroundDriveService.UPLOAD_FAIL;
import static com.worksmobile.wmproject.service.BackgroundDriveService.UPLOAD_SUCCESS;


public class DriveHelper {

    private static final String SUCCESS = "SUCCESS";
    private static final String BASE_URL_API = "https://www.googleapis.com";
    public static final String REDIRECT_URI = "com.worksmobile.wmproject:/oauth2callback";

    public String clientId;
    public String clientSecret;
    private Token token;
    private DriveApi driveApi;
    private Context context;

    public DriveHelper(String clientId, String clientSecret, Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        driveApi = retrofit.create(DriveApi.class);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.context = context;
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

    public Call<Token> createTokenRefeshCall() {
        checkRefreshToken();
        return driveApi.refreshToken(token.getRefreshToken(), clientId, clientSecret, "refresh_token");
    }

    public Call<UploadResult> createUploadCall(String imageLocation, Handler handler) {
        File srcFile = new File(imageLocation);
        if (!srcFile.exists())
            return null;

        String content = "{\"name\": \"" + srcFile.getName() + "\"}";

        RequestBody description = createPartFromString(content);
        MultipartBody.Part dataPart = prepareFilePart("Photo", srcFile, handler);

        return driveApi.uploadFile(getAuthToken(), description, dataPart);
    }

    @NonNull
    private RequestBody createPartFromString(String descriptionString) {
        MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
        return RequestBody.create(contentType, descriptionString);
    }

    @NonNull
    private MultipartBody.Part prepareFilePart(String partName, File file, Handler handler) {
        String mimeType = getMimeType(file);

        RequestBody requestFile = new CustomRequestBody(context, file, mimeType, new CustomRequestBody.ProgressListener() {
            @Override
            public void onUploadProgress(final int progressInPercent, final long totalBytes) {
                System.out.println("Progress : " + progressInPercent);
                if (progressInPercent == 100) {
                    System.out.println("Upload has finished!");
                    Message message = handler.obtainMessage(UPLOAD_SUCCESS, file.getAbsolutePath());

                    handler.sendMessageAtFrontOfQueue(message);
                } else {
                    System.out.println(String.format("%d percent of %d MB", progressInPercent, totalBytes / (1024 * 1024)));
                }
            }

            @Override
            public void onUploadFail() {
                Message message = handler.obtainMessage(UPLOAD_FAIL, file.getAbsolutePath());
                handler.sendMessageAtFrontOfQueue(message);
            }
        });

        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
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

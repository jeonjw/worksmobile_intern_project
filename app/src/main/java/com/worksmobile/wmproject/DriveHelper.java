package com.worksmobile.wmproject;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.retrofit_object.DriveFiles;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.worksmobile.wmproject.service.BackgroundUploadService.UPLOAD_FAIL;
import static com.worksmobile.wmproject.service.BackgroundUploadService.UPLOAD_SUCCESS;


public class DriveHelper {

    private static final int UPDATE_PERCENT_THRESHOLD = 1;
    private static final String SUCCESS = "SUCCESS";
    private static final String BASE_URL_API = "https://www.googleapis.com";
    private static final String REDIRECT_URI = "com.worksmobile.wmproject:/oauth2callback";
    private static final String QUERY_FIELDS = "files/thumbnailLink, files/id, files/name, files/mimeType, files/createdTime, files/imageMediaMetadata, files/videoMediaMetadata";

    private String clientId;
    private String clientSecret;
    private Token token;
    private DriveApi driveApi;
    private Context context;

    public DriveHelper(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        driveApi = retrofit.create(DriveApi.class);
        this.context = context;
        this.clientId = context.getString(R.string.client_id);
        this.clientSecret = null;

        this.token = restoreAuthState();
        checkTokenValidity();
    }

    @Nullable
    private Token restoreAuthState() {
        Gson gson = new Gson();
        String jsonString = context.getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE)
                .getString("TOKEN_STATE", null);

        return gson.fromJson(jsonString, Token.class);
    }

    private void persistAuthState(@NonNull Token token) {
        Gson gson = new Gson();
        String tokenJson = gson.toJson(token);

        context.getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE).edit()
                .putString("TOKEN_STATE", tokenJson)
                .apply();
    }


    private void checkTokenValidity() {
        if (token != null && token.getNeedsTokenRefresh()) {
            executeTokenRefresh();
        }
    }

    private void executeTokenRefresh() {
        System.out.println("TOKEN 갱신 진행");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Call<Token> call = createTokenRefeshCall();
                try {
                    Response<Token> response = call.execute();

                    String message = DriveUtils.printResponse("refreshToken", response);
                    if (message == "SUCCESS") {
                        Token refreshTtoken = response.body();
                        token.setAccessToken(refreshTtoken.getAccessToken());
                        token.setTokenTimeStamp(System.currentTimeMillis());
                        persistAuthState(token);
                    } else {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public void enqueueToeknRequestCall(final TokenCallback callback, String mAuthCode) {
        Call<Token> call = driveApi.getToken(mAuthCode, clientId,
                clientSecret, REDIRECT_URI, "authorization_code");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("enqueueToeknRequestCall", response);
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
                String message = DriveUtils.printFailure("enqueueToeknRequestCall", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void enqueueFileDeleteCall(String fileId, final StateCallback callback) {
        Call<Void> call = driveApi.deleteFile(getAuthToken(), fileId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message = DriveUtils.printResponse("enqueueFileDeleteCall", response);
                if (message == SUCCESS) {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String message = DriveUtils.printFailure("enqueueFileDeleteCall", t);
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
                if (progressInPercent == 100) {
                    System.out.println("Upload has finished!");
                    Message message = handler.obtainMessage(UPLOAD_SUCCESS, file.getAbsolutePath());

                    handler.sendMessageAtFrontOfQueue(message);
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

    public String getExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (!lowerName.contains("."))
            return "";
        return lowerName.substring(lowerName.lastIndexOf(".") + 1);
    }

    public String getMimeType(File file) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String mimeType = map.getMimeTypeFromExtension(getExtension(file.getName()));
        if (TextUtils.isEmpty(mimeType))
            return "*/*";
        return mimeType;
    }

    public void enqueueListCreationCall(String folderId, final ListCallback callback) {
        Call<DriveFiles> call = driveApi.getFiles(getAuthToken(),
                "name", 1000, null, String.format("'%s' in parents", folderId) + " and trashed = false", QUERY_FIELDS);
        call.enqueue(new Callback<DriveFiles>() {
            @Override
            public void onResponse(@NonNull Call<DriveFiles> call, @NonNull Response<DriveFiles> response) {
                String message = DriveUtils.printResponse("enqueueListCreationCall", response);
                if (message == SUCCESS) {
                    if (callback != null) {
                        callback.onSuccess(response.body().getFiles());
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<DriveFiles> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("enqueueListCreationCall", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void enqueueFileInfoCall(String fileId, final StateCallback callback) {
        Call<DriveFile> call = driveApi.getFile(getAuthToken(), fileId, "thumbnailLink");
        call.enqueue(new Callback<DriveFile>() {
            @Override
            public void onResponse(Call<DriveFile> call, Response<DriveFile> response) {
                String message = DriveUtils.printResponse("enqueueFileInfoCall", response);
                if (message == SUCCESS) {
                    if (callback != null) {
                        DriveFile file = response.body();
                        callback.onSuccess(file.getThumbnailLink());
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<DriveFile> call, Throwable t) {
                String message = DriveUtils.printFailure("enqueueFileInfoCall", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public Call<ResponseBody> createDownloadCall(String fileId) {
        return driveApi.downloadFile(getAuthToken(), fileId);
    }

    public void downloadFile(String fileId, String fileName, final StateCallback callback) {
        Call<ResponseBody> call = driveApi.downloadFile(getAuthToken(), fileId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                String message = DriveUtils.printResponse("downloadFile", response);
                if (response.isSuccessful()) {
                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/WorksDrive";

                    File dir = new File(path);
                    if (!dir.exists()) {
                        dir.mkdirs();

                    }
                    File downloadedFile = new File(path, fileName);

                    boolean writtenToDisk = writeResponseBodyToDisk(response.body(), fileName, downloadedFile, callback);
                    if (writtenToDisk)
                        callback.onSuccess(downloadedFile.getAbsolutePath());
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String message = DriveUtils.printFailure("downloadFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String fileName, File destinationFile, StateCallback callback) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();

                inputStream = new BufferedInputStream(body.byteStream(), 4096);
                outputStream = new FileOutputStream(destinationFile);

                int readBuffer;
                long fileSizeDownloadedInByte = 0;
                int fileDownloadedInPercentage = 0;

                while (true) {
                    readBuffer = inputStream.read(fileReader);
                    if (readBuffer == -1) {
                        break;
                    }
                    fileSizeDownloadedInByte += readBuffer;
                    fileDownloadedInPercentage = (int) ((fileSizeDownloadedInByte * 100) / fileSize);

                    callback.onProgressUpdate(fileDownloadedInPercentage);

                    outputStream.write(fileReader, 0, readBuffer);
                    Log.d("TEST", "file download: " + fileDownloadedInPercentage + " of " + 100);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                outputStream.flush();
                DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                dm.addCompletedDownload(fileName, "WM_Project_Google_Drive", true, "image/jpeg", destinationFile.getAbsolutePath(), destinationFile.length(), false);

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}

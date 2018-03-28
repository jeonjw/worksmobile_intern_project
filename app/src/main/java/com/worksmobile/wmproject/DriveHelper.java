package com.worksmobile.wmproject;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.retrofit_object.DriveFiles;
import com.worksmobile.wmproject.retrofit_object.Token;
import com.worksmobile.wmproject.retrofit_object.UploadResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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
    private String accessToken;
    private String refreshToken;

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

    public void listFiles(String folderId, final ListCallback callback) {
        Call<DriveFiles> call = mDriveApi.getFiles(getAuthToken(),
                "name", 1000, null, null);
        call.enqueue(new Callback<DriveFiles>() {
            @Override
            public void onResponse(@NonNull Call<DriveFiles> call, @NonNull Response<DriveFiles> response) {
                String message = DriveUtils.printResponse("listFiles", response);
                if (message == null) {
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
                String message = DriveUtils.printFailure("listFiles", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }


    public void refreshToken(final StateCallback callback) {
        checkRefreshToken();
        Call<Token> call = mDriveApi.refreshToken(refreshToken, clientId, clientSecret, "refresh_token");
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                String message = DriveUtils.printResponse("refreshToken", response);
                if (message == null) {
                    Token token = response.body();
                    accessToken = token.getAccessToken();
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
            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("refreshToken", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void getFile(String fileId, final StateCallback callback) {
        Call<DriveFile> call = mDriveApi.getFile(getAuthToken(), fileId);
        call.enqueue(new Callback<DriveFile>() {
            @Override
            public void onResponse(@NonNull Call<DriveFile> call, @NonNull Response<DriveFile> response) {
                String message = DriveUtils.printResponse("getFile", response);
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
            public void onFailure(@NonNull Call<DriveFile> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("getFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void downloadFile(String fileId, final File dstFile, final StateCallback callback) {
        Call<ResponseBody> call = mDriveApi.downloadFile(getAuthToken(), fileId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                String message = DriveUtils.printResponse("downloadFile", response);
                if (message == null) {
                    BufferedInputStream bis = new BufferedInputStream(response.body().byteStream());
                    BufferedOutputStream bos = null;
                    try {
                        bos = new BufferedOutputStream(new FileOutputStream(dstFile));
                        int length;
                        byte[] buffer = new byte[1024];
                        while ((length = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, length);
                        }
                        bos.flush();
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onFailure(Log.getStackTraceString(e));
                        }
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (bos != null)
                                bos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("downloadFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void uploadFile(File srcFile, String folderId, StateCallback callback) {
        uploadFile(srcFile, srcFile.getName(), folderId, callback);
    }

    public void uploadFile(File srcFile, String title, final String folderId, final StateCallback callback) {
        System.out.println("존재여부" + srcFile.exists());
        MediaType contentType = MediaType.parse("application/json; charset=UTF-8");
        String content = "{\"name\": \"" + title + "\"}";
        MultipartBody.Part metaPart = MultipartBody.Part.create(RequestBody.create(contentType, content));
        String mimeType = getMimeType(srcFile);
        MultipartBody.Part dataPart = MultipartBody.Part.create(RequestBody.create(MediaType.parse(mimeType), srcFile));

        Call<UploadResult> call = mDriveApi.uploadFile(getAuthToken(), metaPart, dataPart);
        call.enqueue(new Callback<UploadResult>() {
            @Override
            public void onResponse(@NonNull Call<UploadResult> call, @NonNull Response<UploadResult> response) {
                String message = DriveUtils.printResponse("uploadFile", response);
                if (message == null) {
                    moveFile(response.body().getId(), folderId, new StateCallback() {
                        @Override
                        public void onSuccess() {
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String msg) {
                            if (callback != null) {
                                callback.onFailure(msg);
                            }
                        }
                    });
                } else {
                    if (callback != null) {
                        callback.onFailure(message);
                    }
                }
            }

            @Override
            public void onFailure(Call<UploadResult> call, Throwable t) {
                String message = DriveUtils.printFailure("uploadFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void moveFile(String fileId, String dstFolderId, StateCallback callback) {
        moveFile(fileId, dstFolderId, "root", callback);
    }


    public void moveFile(String fileId, String dstFolderId, String srcFolderId, final StateCallback callback) {
        Call<DriveFile> call = mDriveApi.moveFile(getAuthToken(), fileId, dstFolderId, srcFolderId);
        call.enqueue(new Callback<DriveFile>() {
            @Override
            public void onResponse(@NonNull Call<DriveFile> call, @NonNull Response<DriveFile> response) {
                String message = DriveUtils.printResponse("moveFile", response);
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
            public void onFailure(@NonNull Call<DriveFile> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("moveFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void deleteFile(String fileId, final StateCallback callback) {
        Call<Void> call = mDriveApi.deleteFile(getAuthToken(), fileId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message = DriveUtils.printResponse("deleteFile", response);
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
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("deleteFile", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    private String getAuthToken() {
        checkAccessToken();
        return String.format("Bearer %s", accessToken);
    }

    private void checkRefreshToken() {
        if (refreshToken == null) {
            throw new IllegalStateException("Refresh token is null!");
        }
    }

    private void checkAccessToken() {
        if (accessToken == null) {
            throw new IllegalStateException("Access token is null!");
        }
    }

    public void setAccessToken(String mAccessToken) {
        this.accessToken = mAccessToken;
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

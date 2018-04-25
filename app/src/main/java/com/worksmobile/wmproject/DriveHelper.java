package com.worksmobile.wmproject;

import android.content.Context;
import android.support.media.ExifInterface;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.value_object.DriveFile;
import com.worksmobile.wmproject.value_object.DriveFiles;
import com.worksmobile.wmproject.value_object.LocationInfo;
import com.worksmobile.wmproject.value_object.Token;
import com.worksmobile.wmproject.value_object.UploadResult;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.util.DriveUtils;
import com.worksmobile.wmproject.util.FileUtils;

import java.io.File;
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

import static android.support.media.ExifInterface.TAG_GPS_ALTITUDE;
import static android.support.media.ExifInterface.TAG_GPS_LATITUDE;
import static android.support.media.ExifInterface.TAG_GPS_LATITUDE_REF;
import static android.support.media.ExifInterface.TAG_GPS_LONGITUDE;
import static android.support.media.ExifInterface.TAG_GPS_LONGITUDE_REF;
import static com.worksmobile.wmproject.service.BackgroundUploadService.UPLOAD_FAIL;
import static com.worksmobile.wmproject.service.BackgroundUploadService.UPLOAD_SUCCESS;


public class DriveHelper {

    private static final String SUCCESS = "SUCCESS";
    private static final String BASE_URL_API = "https://www.googleapis.com";
    private static final String REDIRECT_URI = "com.worksmobile.wmproject:/oauth2callback";
    private static final String QUERY_FIELDS = "files/properties, files/thumbnailLink, files/id, files/name, files/mimeType, files/createdTime, files/size, files/imageMediaMetadata, files/videoMediaMetadata";

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
                    synchronized (DriveHelper.this) {
                        DriveHelper.this.notify();
                    }
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

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

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

    public void enqueuePermanentDeleteCall(String fileId, final StateCallback callback) {
        Call<Void> call = driveApi.deleteFile(getAuthToken(), fileId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                String message = DriveUtils.printResponse("enqueuePermanentDeleteCall", response);
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
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("enqueuePermanentDeleteCall", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void enqueueDeleteCall(DriveFile file, final StateCallback callback) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("trashed", true);

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

        Call<DriveFile> call = driveApi.updateFile(getAuthToken(), file.getId(), body);
        call.enqueue(new Callback<DriveFile>() {
            @Override
            public void onResponse(@NonNull Call<DriveFile> call, @NonNull Response<DriveFile> response) {
                String message = DriveUtils.printResponse("enqueuePermanentDeleteCall", response);
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
            public void onFailure(@NonNull Call<DriveFile> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("enqueuePermanentDeleteCall", t);
                if (callback != null) {
                    callback.onFailure(message);
                }
            }
        });
    }

    public void setLocationProperties(DriveFile file, final StateCallback callback) {
        JsonObject jsonObject = new JsonObject();
        JsonObject properties = new JsonObject();
        if (file.getImageMediaMetadata().getLocationInfo() != null) {
            properties.addProperty("hasLocateInfo", true);
            properties.addProperty("longitude", file.getImageMediaMetadata().getLocationInfo().getLongitude());
            properties.addProperty("latitude", file.getImageMediaMetadata().getLocationInfo().getLatitude());
            properties.addProperty("altitude", file.getImageMediaMetadata().getLocationInfo().getAltitude());
            jsonObject.add("properties", properties);
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

        Call<DriveFile> call = driveApi.updateFile(getAuthToken(), file.getId(), body);
        call.enqueue(new Callback<DriveFile>() {
            @Override
            public void onResponse(@NonNull Call<DriveFile> call, @NonNull Response<DriveFile> response) {
                String message = DriveUtils.printResponse("setLocationProperties", response);
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
            public void onFailure(@NonNull Call<DriveFile> call, @NonNull Throwable t) {
                String message = DriveUtils.printFailure("setLocationProperties", t);
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

    public Call<UploadResult> createUploadCall(FileStatus fileStatus, Handler handler) {
        File srcFile = new File(fileStatus.getLocation());
        if (!srcFile.exists())
            return null;

        LocationInfo locationInfo = getLocationFromEXIF(fileStatus.getLocation());

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", srcFile.getName());

        if (locationInfo != null) {
            JsonObject properties = new JsonObject();
            properties.addProperty("hasLocateInfo", true);
            properties.addProperty("latitude", locationInfo.getLatitude());
            properties.addProperty("longitude", locationInfo.getLongitude());
            properties.addProperty("altitude", locationInfo.getAltitude());
            jsonObject.add("properties", properties);
        }
        RequestBody propertiyBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        MultipartBody.Part dataPart = prepareFilePart(srcFile, fileStatus, handler);

        return driveApi.uploadFile(getAuthToken(), propertiyBody, dataPart);
    }

    public LocationInfo getLocationFromEXIF(String file) {
        double latitudeDegree = 0;
        double longitudeDegree = 0;
        double altitudeDegree = 0;
        LocationInfo locationInfo = null;
        try {
            ExifInterface exifInterface = new ExifInterface(file);

            String latitude = exifInterface.getAttribute(TAG_GPS_LATITUDE);
            String longitude = exifInterface.getAttribute(TAG_GPS_LONGITUDE);
            String latitudeRef = exifInterface.getAttribute(TAG_GPS_LATITUDE_REF);
            String longitudeRef = exifInterface.getAttribute(TAG_GPS_LONGITUDE_REF);
            altitudeDegree = exifInterface.getAttributeDouble(TAG_GPS_ALTITUDE, 0);

            if (latitudeRef != null || longitudeRef != null) {
                if (latitudeRef.equals("N")) {
                    System.out.println(convertToDegree(latitude));
                    latitudeDegree = convertToDegree(latitude);
                } else {
                    System.out.println(0 - convertToDegree(latitude));
                    latitudeDegree = 0 - convertToDegree(latitude);
                }

                if (longitudeRef.equals("E")) {
                    System.out.println(convertToDegree(longitude));
                    longitudeDegree = convertToDegree(longitude);
                } else {
                    System.out.println(0 - convertToDegree(longitude));
                    longitudeDegree = 0 - convertToDegree(longitude);
                }

                locationInfo = new LocationInfo(latitudeDegree, longitudeDegree, altitudeDegree);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("GPS DEGREE : " + latitudeDegree + ", " + longitudeDegree + ", " + altitudeDegree);
        return locationInfo;
    }

    private double convertToDegree(String stringDMS) {
        double result;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        Double D0 = Double.valueOf(stringD[0]);
        Double D1 = Double.valueOf(stringD[1]);
        Double doubleD = D0 / D1;

        String[] stringM = DMS[1].split("/", 2);
        Double M0 = Double.valueOf(stringM[0]);
        Double M1 = Double.valueOf(stringM[1]);
        Double doubleM = M0 / M1;

        String[] stringS = DMS[2].split("/", 2);
        Double S0 = Double.valueOf(stringS[0]);
        Double S1 = Double.valueOf(stringS[1]);
        Double doubleS = S0 / S1;

        result = (float) (doubleD + (doubleM / 60) + (doubleS / 3600));
        return result;
    }

    @NonNull
    private MultipartBody.Part prepareFilePart(File file, FileStatus fileStatus, Handler
            handler) {
        String mimeType = FileUtils.getMimeType(file);

        RequestBody requestFile = new CustomRequestBody(context, file, mimeType, new CustomRequestBody.ProgressListener() {
            @Override
            public void onUploadProgress(final int progressInPercent, final long totalBytes) {
                if (progressInPercent == 100) {
                    Message message = handler.obtainMessage(UPLOAD_SUCCESS, fileStatus);

                    handler.sendMessageAtFrontOfQueue(message);
                }
            }

            @Override
            public void onUploadFail() {
                Message message = handler.obtainMessage(UPLOAD_FAIL, file.getAbsolutePath());
                handler.sendMessageAtFrontOfQueue(message);
            }
        });

        return MultipartBody.Part.createFormData("data", file.getName(), requestFile);
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


    public void enqueueListCreationCall(boolean trash, String mimeType, final ListCallback callback) {
        String query = String.format("'%s' in parents", "root") + " and trashed = " + String.valueOf(trash);
        if (mimeType != null) {
            query += String.format(" and mimeType contains '%s'", mimeType);
        }

        Call<DriveFiles> call = driveApi.getFiles(getAuthToken(),
                "name", 1000, null, query, QUERY_FIELDS);
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

    public void enqueuePhotoMapListCreationCall(final ListCallback callback) {
        String query = String.format("'%s' in parents", "root") + " and trashed = " + String.valueOf(false);
        query += String.format(" and mimeType contains '%s'", "image/");
        query += " and properties has { key='hasLocateInfo' and value = 'true' }";


        Call<DriveFiles> call = driveApi.getFiles(getAuthToken(),
                "name", 1000, null, query, QUERY_FIELDS);
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
            public void onResponse(@NonNull Call<DriveFile> call, @NonNull Response<DriveFile> response) {
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
            public void onFailure(@NonNull Call<DriveFile> call, @NonNull Throwable t) {
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

}

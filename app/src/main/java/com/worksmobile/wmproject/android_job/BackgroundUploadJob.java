package com.worksmobile.wmproject.android_job;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.StateCallback;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.io.File;

public class BackgroundUploadJob extends Job {
    public static final String TAG = "BackgroundUploadJob";
    private Context context;
    private AuthState authState;

    public static final String TEST_FOLDER_ID = "0BzHAMvdMNu8aSFNBVFdhWldFM2c";
    private DriveHelper mDriveHelper;
    private static int jobId;

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        this.context = getContext();
        authState = restoreAuthState();

        if (mDriveHelper == null)
            mDriveHelper = new DriveHelper(context.getString(R.string.client_id), context.getString(R.string.client_secret));

        if (authState != null) {
            if (authState.getNeedsTokenRefresh())
                refreshToken();
            mDriveHelper.setAccessToken(authState.getAccessToken());
        }
        createNotification();

        return Result.SUCCESS;
    }


    private void persistAuthState(@NonNull AuthState authState) {
        context.getSharedPreferences("AuthStatePreference", Context.MODE_PRIVATE).edit()
                .putString("AUTH_STATE", authState.toJsonString())
                .apply();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = context.getSharedPreferences("AuthStatePreference", Context.MODE_PRIVATE)
                .getString("AUTH_STATE", null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }


    private void refreshToken() {
        AuthorizationService service = new AuthorizationService(context);
        service.performTokenRequest(authState.createTokenRefreshRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                if (exception != null) {
                    Log.w("TAG", "Token Exchange failed", exception);
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, null);
                        persistAuthState(authState);
                        Log.i("TAG", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                    }
                }
            }
        });
    }

    private void createNotification() {
        NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification notification = new Notification.Builder(context, "WM")
                    .setContentTitle("Some Message")
                    .setContentText("You've received new messages!")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();

            if (notificationManager != null) {
                notificationManager.notify(0, notification);
            }

        } else {
            System.out.println("버전 오레오 미만");

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("My notification")
                            .setContentText("Hello World!")
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setPriority(Notification.PRIORITY_HIGH);

            if (notificationManager != null) {
                notificationManager.notify(0, mBuilder.build());
            }
        }

        getLastPicture();
    }

    @Override
    protected void onReschedule(int newJobId) {
        super.onReschedule(newJobId);
        System.out.println("Reschedule TTTTT");
    }

    private void getLastPicture() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            System.out.println("LOCATION : " + imageLocation);
            mDriveHelper.uploadFile(new File(imageLocation), TEST_FOLDER_ID, new StateCallback() {
                @Override
                public void onSuccess() {
                    System.out.println("업로드 성공");
                    //Local DB에서 업로드 목록 제거

                }

                @Override
                public void onFailure(String msg) {
                    System.out.println("업로드 실패 뭔가 정책을 정해서 결정할것");
                }
            });
        }
    }

    public static void scheduleJob() {
       jobId = new JobRequest.Builder(BackgroundUploadJob.TAG)
                .setExecutionWindow(10L, 30000L)
                .setBackoffCriteria(1000, JobRequest.BackoffPolicy.LINEAR)
                .setRequiredNetworkType(JobRequest.NetworkType.UNMETERED)
                .setRequiresDeviceIdle(false)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}

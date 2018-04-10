package com.worksmobile.wmproject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class CustomRequestBody extends RequestBody {

    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final int UPDATE_PERCENT_THRESHOLD = 1;

    private File file;
    private ProgressListener listener;
    private MediaType mediaType;
    private Context context;
    private ConnectivityManager connectivityManager;

    public interface ProgressListener {
        void onUploadProgress(int progressInPercent, long totalBytes);
    }

    public CustomRequestBody(Context context, File file, String type, ProgressListener listener) {
        this.context = context;
        this.file = file;
        this.mediaType = MediaType.parse(type);
        this.listener = listener;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return file.length();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long totalBytes = file.length();

        try (FileInputStream in = new FileInputStream(file)) {
            // init variables
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            long uploadedBytes = 0;
            int readBytes;
            int fileUploadedInPercent = 0;

            // go through the file and notify the UI
            while ((readBytes = in.read(buffer)) != -1) {
                if (!isUnMeteredNetWork()) {
                    break;
                }
                int newfileUploadedInPercent = (int) ((uploadedBytes * 100) / totalBytes);
                if (fileUploadedInPercent + UPDATE_PERCENT_THRESHOLD <= newfileUploadedInPercent) {
                    fileUploadedInPercent = newfileUploadedInPercent;
                    listener.onUploadProgress(newfileUploadedInPercent, totalBytes);
                }

                uploadedBytes += readBytes;
                sink.write(buffer, 0, readBytes);
            }
        }

        listener.onUploadProgress(100, totalBytes);
    }

    private boolean isUnMeteredNetWork() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX)
                return true;
        }
        return false;
    }
}
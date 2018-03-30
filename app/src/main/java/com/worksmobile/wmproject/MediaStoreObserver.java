package com.worksmobile.wmproject;


import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

public class MediaStoreObserver extends ContentObserver {

    private int storageCount;
    private static final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private Context context;
    private Cursor countCursor;

    public MediaStoreObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;

        countCursor = context.getContentResolver().query(uri,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        if (countCursor != null) {
            countCursor.moveToFirst();
            storageCount = countCursor.getInt(0);
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        int previousCount = storageCount;

        countCursor = context.getContentResolver().query(uri,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        if (countCursor != null) {
            countCursor.moveToFirst();
            storageCount = countCursor.getInt(0);
        }

        if (storageCount > previousCount) {
            System.out.println("사진추가");
            sendDriveBroadCast();
        } else {
            System.out.println("사진 추가는 아님");
        }

        System.out.println(" Observer : 스토리지 갯수 : " + storageCount);
        countCursor.close();
    }

    private void sendDriveBroadCast() {
        context.sendBroadcast(new Intent("com.worksmobile.wm_project.NEW_MEDIA"), Manifest.permission.NEW_MEDIA);
    }

}
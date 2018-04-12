package com.worksmobile.wmproject.content_observer;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

import com.worksmobile.wmproject.ContractDB;
import com.worksmobile.wmproject.DBHelpler;
import com.worksmobile.wmproject.MyBroadCastReceiver;

public class MediaStoreObserver extends ContentObserver {

    private int storageCount;
    private Uri EXTERNAL_CONTENT_URI;
    private Context context;
    private Cursor countCursor;
    private DBHelpler dbHelper;
    private Handler handler;
    private static final int CALLBACK_PRESENT_INTEGER = 0;
    private Runnable sendBroakdCastTask;


    public MediaStoreObserver(Handler handler, Context context, Uri contentUri) {
        super(handler);
        this.context = context;
        this.handler = handler;
        initDB();

        EXTERNAL_CONTENT_URI = contentUri;

        countCursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
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
        int previousCount = storageCount;
        countCursor = context.getContentResolver().query(EXTERNAL_CONTENT_URI,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        if (countCursor != null) {
            countCursor.moveToFirst();
            storageCount = countCursor.getInt(0);
        }

        if (storageCount > previousCount) {
            System.out.println("Media 추가");
            wirteToDatabase(getLastPictureLocation());

            if (handler.hasMessages(CALLBACK_PRESENT_INTEGER) && sendBroakdCastTask != null) {
                handler.removeCallbacks(sendBroakdCastTask);
            }

            sendBroakdCastTask = sendDriveBroadCast();
            handler.postDelayed(sendBroakdCastTask, 5000);

        }
        System.out.println(" Observer : 스토리지 갯수 : " + storageCount);
        countCursor.close();
    }


    private void initDB() {
        System.out.println("INIT DB");
        dbHelper = new DBHelpler(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
//        db.execSQL("DROP TABLE IF EXISTS " + "UPLOAD_TABLE");
//        db.execSQL(ContractDB.SQL_CREATE_TBL);
        db.execSQL(ContractDB.SQL_DELETE);
        db.execSQL("UPDATE SQLITE_SEQUENCE SET seq = 0" + " WHERE name = 'UPLOAD_TABLE'");

    }

    private void wirteToDatabase(String uri) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ContractDB.COL_LOCATION, uri);
        values.put(ContractDB.COL_STATUS, "UPLOAD");

        db.insert(ContractDB.TBL_CONTACT, null, values);
        db.close();
    }

    private Runnable sendDriveBroadCast() {
        return new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(CALLBACK_PRESENT_INTEGER);
                Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
                intent.setClass(context, MyBroadCastReceiver.class);
                context.sendBroadcast(intent);
            }
        };
    }


    private String getLastPictureLocation() {
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
            return cursor.getString(1);
        }

        cursor.close();

        return null;
    }
}
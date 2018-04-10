package com.worksmobile.wmproject;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

public class MediaStoreObserver extends ContentObserver {

    private int storageCount;
    private static final Uri EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private Context context;
    private Cursor countCursor;
    private DBHelpler dbHelper = null;
    private Handler handler;
    private static final int CALLBACK_PRESENT_INTEGER = 0;
    private Runnable sendBroakdCastTask;


    public MediaStoreObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
        this.handler = handler;
        initDB();

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
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);

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
            wirteToDatabase(getRealPathFromUri(uri));

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
    }

    private Runnable sendDriveBroadCast() {
        return () -> {
            handler.sendEmptyMessage(CALLBACK_PRESENT_INTEGER);
            Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
            intent.setClass(context, MyBroadCastReceiver.class);
            context.sendBroadcast(intent);
        };
    }

    public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
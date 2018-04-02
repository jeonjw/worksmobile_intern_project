package com.worksmobile.wmproject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBHelpler extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DBFILE_CONTACT = "uploadList.db";

    public DBHelpler(Context context) {
        super(context, DBFILE_CONTACT, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(ContractDB.SQL_CREATE_TBL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }
}

package com.worksmobile.wmproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;


public class DBHelpler extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DBFILE_CONTACT = "uploadList.db";
    private SQLiteDatabase writableDatabase;
    private SQLiteDatabase readableDatabase;

    public DBHelpler(Context context) {
        super(context, DBFILE_CONTACT, null, DB_VERSION);
        writableDatabase = getWritableDatabase();
        readableDatabase = getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(ContractDB.SQL_CREATE_TBL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        onCreate(sqLiteDatabase);
    }

    public void updateDB(String column, String columnValue, String where, String whereArgs) {
        ContentValues values = new ContentValues();
        values.put(column, columnValue);
        writableDatabase.update(ContractDB.TBL_CONTACT, values, where + "=?", new String[]{whereArgs});
    }

    public void deleteDB(String where, String whereArgs) {
        writableDatabase.delete(ContractDB.TBL_CONTACT, where + "=?", new String[]{whereArgs});
    }

    public void insertDB(String location, String status) {

        ContentValues values = new ContentValues();
        values.put(ContractDB.COL_LOCATION, location);
        values.put(ContractDB.COL_STATUS, status);
        values.put(ContractDB.COL_DATE, new Date().toString());

        writableDatabase.insert(ContractDB.TBL_CONTACT, null, values);
    }

    public void closeDB() {
        writableDatabase.close();
        readableDatabase.close();
    }
}

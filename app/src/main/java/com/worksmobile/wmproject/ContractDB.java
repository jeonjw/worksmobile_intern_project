package com.worksmobile.wmproject;

public class ContractDB {

    private ContractDB() {
    }

    public static final String TBL_CONTACT = "UPLOAD_TABLE";

    public static final String COL_LOACTION = "LOCATION";
    public static final String COL_ID = "_id";

    public static final String SQL_CREATE_TBL = "CREATE TABLE IF NOT EXISTS " + TBL_CONTACT + " " +
            "(" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COL_LOACTION + " TEXT" + ")";


    public static final String SQL_SELECT = "SELECT * FROM " + TBL_CONTACT;

    public static final String SQL_DELETE = "DELETE FROM " + TBL_CONTACT;



}

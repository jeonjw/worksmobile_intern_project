package com.worksmobile.wmproject;

public class ContractDB {

    private ContractDB() {
    }

    public static final String TBL_CONTACT = "UPLOAD_TABLE";

    public static final String COL_LOCATION = "LOCATION";
    public static final String COL_STATUS = "STATUS";
    public static final String COL_DATE = "DATE";
    public static final String COL_ID = "_id";

    public static final String SQL_CREATE_TBL = "CREATE TABLE IF NOT EXISTS " + TBL_CONTACT + " " +
            "(" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COL_LOCATION + " TEXT," +
            COL_STATUS + " TEXT," +
            COL_DATE + " TEXT" + ")";


    public static final String SQL_SELECT_ALL = "SELECT * FROM " + TBL_CONTACT;
    public static final String SQL_SELECT_UPLOAD = "SELECT " + COL_ID + ", " + COL_LOCATION + ", " + COL_DATE + " FROM " + TBL_CONTACT + " WHERE STATUS='UPLOAD'";
    public static final String SQL_SELECT_DOWNLOAD = "SELECT " + COL_ID + ", " + COL_LOCATION + ", " + COL_DATE + " FROM " + TBL_CONTACT + " WHERE STATUS='DOWNLOAD'";

    public static final String SQL_DELETE = "DELETE FROM " + TBL_CONTACT;


}

package com.worksmobile.wmproject.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "FILE_STAUTS")
public class FileStatus {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "LOCATION")
    private String location;

    @ColumnInfo(name = "DATE")
    private String date;

    @ColumnInfo(name = "STATUS")
    private String status;

    public FileStatus(String location, String date, String status) {
        this.location = location;
        this.date = date;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

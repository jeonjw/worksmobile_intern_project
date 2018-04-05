package com.worksmobile.wmproject.retrofit_object;

import com.google.gson.annotations.SerializedName;

public class UploadResult {

    @SerializedName("name")
    private String name;
    @SerializedName("id")
    private String id;
    private String databaseId;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    @Override
    public String toString() {
        return "UploadResult <" + "name = " + name + ", id = " + id + "name = " + name +'>';
    }
}

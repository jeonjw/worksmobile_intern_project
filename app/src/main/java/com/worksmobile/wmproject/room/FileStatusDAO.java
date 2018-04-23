package com.worksmobile.wmproject.room;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface FileStatusDAO {

    @Query("SELECT * FROM FILE_STAUTS")
    List<FileStatus> getAll();

    @Query("SELECT * FROM FILE_STAUTS WHERE STATUS='UPLOAD'")
    List<FileStatus> getUploadFileList();

    @Query("SELECT * FROM FILE_STAUTS WHERE STATUS='DOWNLOAD'")
    List<FileStatus> getDownloadFileList();

    @Query("SELECT * FROM FILE_STAUTS WHERE (STATUS='DOWNLOAD' OR STATUS='UPLOADED') AND LOCATION LIKE '%' || :fileName || '%'")
    List<FileStatus> findPath(String fileName);

    @Insert
    void insertFileStatus(FileStatus... fileStatuses);

    @Update
    void updateFileStatus(FileStatus fileStatuses);

    @Delete
    void deleteFileStatus(FileStatus fileStatuses);


}

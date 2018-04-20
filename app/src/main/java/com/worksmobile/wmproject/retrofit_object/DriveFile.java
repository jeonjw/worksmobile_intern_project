package com.worksmobile.wmproject.retrofit_object;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class DriveFile implements Serializable {

    @SerializedName("kind")
    private String kind;
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("mimeType")
    private String mimeType;
    @SerializedName("description")
    private String description;
    @SerializedName("starred")
    private boolean starred;
    @SerializedName("trashed")
    private boolean trashed;
    @SerializedName("explicitlyTrashed")
    private boolean explicitlyTrashed;
    @SerializedName("hasThumbnail")
    private boolean hasThumbnail;
    @SerializedName("thumbnailLink")
    private String thumbnailLink;
    @SerializedName("parents")
    private String[] parents;
    @SerializedName("createdTime")
    private Date createdTime;
    @SerializedName("modifiedTime")
    private Date modifiedTime;
    @SerializedName("shared")
    private boolean shared;
    @SerializedName("fileExtension")
    private String fileExtension;
    @SerializedName("size")
    private long size;
    @SerializedName("imageMediaMetadata")
    private MediaMetadata imageMediaMetadata;
    @SerializedName("videoMediaMetadata")
    private MediaMetadata videoMediaMetadata;

    private int progress;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public MediaMetadata getImageMediaMetadata() {
        return imageMediaMetadata;
    }

    public MediaMetadata getVideoMediaMetadata() {
        return videoMediaMetadata;
    }

    public void setVideoMediaMetadata(MediaMetadata videoMediaMetadata) {
        this.videoMediaMetadata = videoMediaMetadata;
    }

    public int getWidth() {
        if (imageMediaMetadata == null)
            return videoMediaMetadata.getWidth();
        else

            return imageMediaMetadata.getWidth();
    }

    public int getHeight() {
        if (imageMediaMetadata == null)
            return videoMediaMetadata.getWidth();
        else
            return imageMediaMetadata.getHeight();
    }

    public String getTakenTime() {
        if (imageMediaMetadata == null)
            return null;
        return imageMediaMetadata.getTime();
    }

    public void setImageMediaMetadata(MediaMetadata imageMediaMetadata) {
        this.imageMediaMetadata = imageMediaMetadata;
    }


    private boolean isSelected;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setTrashed(boolean trashed) {
        this.trashed = trashed;
    }

    public boolean isExplicitlyTrashed() {
        return explicitlyTrashed;
    }

    public void setExplicitlyTrashed(boolean explicitlyTrashed) {
        this.explicitlyTrashed = explicitlyTrashed;
    }

    public String[] getParents() {
        return parents;
    }

    public void setParents(String[] parents) {
        this.parents = parents;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isHasThumbnail() {
        return hasThumbnail;
    }

    public void setHasThumbnail(boolean hasThumbnail) {
        this.hasThumbnail = hasThumbnail;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }

    @Override
    public String toString() {
        if (imageMediaMetadata == null && videoMediaMetadata != null)
            return "DriveFile <" + "id = " + id +
                    ", name = " + name + ", mimeType = " + mimeType + ", thumbnailLink = " + thumbnailLink + ", createdTime = " + createdTime.getTime() + ", size = " + size + ", metadata : " + videoMediaMetadata.toString() + '>';
        else if (imageMediaMetadata != null && videoMediaMetadata == null)
            return "DriveFile <" + "id = " + id +
                    ", name = " + name + ", mimeType = " + mimeType + ", thumbnailLink = " + thumbnailLink + ", createdTime = " + createdTime.getTime() + ", size = " + size + ", metadata : " + imageMediaMetadata.toString() + '>';
        else
            return "DriveFile <" + "id = " + id +
                    ", name = " + name + ", mimeType = " + mimeType + ", thumbnailLink = " + thumbnailLink + ", size = " + size + '>';
    }
}

package com.worksmobile.wmproject.value_object;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.google.gson.annotations.SerializedName;
import com.worksmobile.wmproject.BR;

import java.io.Serializable;
import java.util.Date;

public class DriveFile extends BaseObservable implements Serializable {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("mimeType")
    private String mimeType;
    @SerializedName("properties")
    private Properties properties;
    @SerializedName("trashed")
    private boolean trashed;
    @SerializedName("explicitlyTrashed")
    private boolean explicitlyTrashed;
    @SerializedName("hasThumbnail")
    private boolean hasThumbnail;
    @SerializedName("thumbnailLink")
    private String thumbnailLink;
    @SerializedName("createdTime")
    private Date createdTime;
    @SerializedName("modifiedTime")
    private Date modifiedTime;
    @SerializedName("size")
    private long size;
    @SerializedName("imageMediaMetadata")
    private MediaMetadata imageMediaMetadata;
    @SerializedName("videoMediaMetadata")
    private MediaMetadata videoMediaMetadata;


    private boolean checked;

    @Bindable
    public boolean isChecked() {
        return checked;
    }

    @Bindable
    public void setChecked(boolean checked) {
        this.checked = checked;
        notifyPropertyChanged(BR.checked);
    }

    public void changeCheckedValue() {
        checked ^= true;
        notifyPropertyChanged(BR.checked);
    }

    public MediaMetadata getImageMediaMetadata() {
        return imageMediaMetadata;
    }

    public MediaMetadata getVideoMediaMetadata() {
        return videoMediaMetadata;
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

    public Properties getProperties() {
        return properties;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        DriveFile file = (DriveFile) obj;
        if (this.id.equals(file.getId())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }


}

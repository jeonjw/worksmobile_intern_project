package com.worksmobile.wmproject;

import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

import com.worksmobile.wmproject.util.ImageUtil;
import com.worksmobile.wmproject.value_object.DriveFile;

public class BindingAdapters {
    @BindingAdapter("bind:item")
    public static void bindItem(RecyclerView recyclerView, ObservableArrayList<DriveFile> fileList) {
        ThumbnailRecyclerViewAdapter adapter = (ThumbnailRecyclerViewAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.setFileList(fileList);
        }
    }

    @BindingAdapter({"bind:imageUrl"})
    public static void loadImage(ImageView imageView, DriveFile file) {
        String mimeType = file.getMimeType();
        if (file.getThumbnailLink() != null) {
            if (mimeType.contains("image") || mimeType.contains("video")) {
                String thumbnailLink = ImageUtil.replaceThumbnailSize(file.getThumbnailLink(), ImageUtil.calculateProperThumbnailSize(file.getWidth() * 3 / 20, file.getHeight() * 3 / 20));
                ImageUtil.loadImageWithSizeOverride(imageView, thumbnailLink, file.getWidth() * 3 / 20, file.getHeight() * 3 / 20);
            } else {
                ImageUtil.loadImageWithUrl(imageView, file.getThumbnailLink());
            }
        } else { //섬네일 링크가 없을 때
            int imageId = mimeType.contains("video") ? R.drawable.video_default : R.drawable.image_default;
            ImageUtil.loadImageWithResourceId(imageView, imageId);
        }
    }


}

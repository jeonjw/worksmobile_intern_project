package com.worksmobile.wmproject;

import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;

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
                String thumbnailLink = replaceThumbnailSize(file.getThumbnailLink(), calculateProperThumbnailSize(file.getWidth(), file.getHeight()));
                GlideApp.with(imageView)
                        .load(thumbnailLink)
                        .override(file.getWidth() * 3 / 20, file.getHeight() * 3 / 20)
                        .centerInside()
                        .into(imageView);
            } else {
                GlideApp.with(imageView)
                        .load(file.getThumbnailLink())
                        .centerInside()
                        .into(imageView);
            }
        } else { //섬네일 링크가 없을 때
            int imageId = mimeType.contains("video") ? R.drawable.video_default : R.drawable.image_default;
            GlideApp.with(imageView)
                    .load(imageId)
                    .centerInside()
                    .into(imageView);
        }
    }

    public static String replaceThumbnailSize(String string, String replacement) {
        int pos = string.lastIndexOf("s220");
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + "s220".length(), string.length());
        } else {
            return string;
        }
    }

    private static String calculateProperThumbnailSize(int width, int height) {
        String properWidth = String.valueOf(width * 3 / 20);
        String properHeight = String.valueOf(height * 3 / 20);
        return "w" + properWidth + "-" + "h" + properHeight;

    }
}

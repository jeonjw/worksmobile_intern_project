package com.worksmobile.wmproject.util;

import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.worksmobile.wmproject.GlideApp;

public class ImageUtil {
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

    public static String calculateProperThumbnailSize(int width, int height) {
        String properWidth = String.valueOf(width);
        String properHeight = String.valueOf(height);
        return "w" + properWidth + "-" + "h" + properHeight;
    }

    public static void loadImageWithUrl(ImageView imageView, String url) {
        GlideApp.with(imageView)
                .load(url)
                .centerInside()
                .into(imageView);

        RequestManager requestManager = GlideApp.with(imageView);
    }

    public static void loadImageWithSizeOverride(ImageView imageView, String url, int width, int height) {
        GlideApp.with(imageView)
                .load(url)
                .override(width, height)
                .centerInside()
                .into(imageView);
    }

    public static void loadImageWithResourceId(ImageView imageView, int resourceId) {
        GlideApp.with(imageView)
                .load(resourceId)
                .centerInside()
                .into(imageView);
    }
}

package com.worksmobile.wmproject;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.worksmobile.wmproject.value_object.DriveFile;

import java.util.List;

public class ViewerPagerAdapter extends PagerAdapter {

    private LayoutInflater mLayoutInflater;
    private List<DriveFile> fileList;

    public ViewerPagerAdapter(Context context, List<DriveFile> thumbnailLinkList) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.fileList = thumbnailLinkList;

    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false);
        ImageView imageView = itemView.findViewById(R.id.image_viewer_imageview);

        DriveFile file = fileList.get(position);
        String thumbnailLink = replaceThumbnailSize(file.getThumbnailLink(), calculateProperThumbnailSize(file.getWidth(), file.getHeight()));
        GlideApp.with(imageView.getContext())
                .load(thumbnailLink)
                .into(imageView);

        container.addView(itemView);
        return itemView;
    }

    @Override
    public int getItemPosition(Object object) {
        if (fileList.contains(object)) {
            return fileList.indexOf(object);
        } else {
            return POSITION_NONE;
        }
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout) object);
    }

    public String replaceThumbnailSize(String string, String properSize) {
        int pos = string.lastIndexOf("s220");
        if (pos > -1) {
            return string.substring(0, pos)
                    + properSize
                    + string.substring(pos + "s220".length(), string.length());
        } else {
            return string;
        }
    }

    private String calculateProperThumbnailSize(int width, int height) {
        String properWidth = String.valueOf(width * 8 / 10);
        String properHeight = String.valueOf(height * 8 / 10);

        return "w" + properWidth + "-" + "h" + properHeight;

    }
}

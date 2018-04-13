package com.worksmobile.wmproject;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.List;

public class ViewerPagerAdapter extends PagerAdapter {

    private LayoutInflater mLayoutInflater;
    private List<DriveFile> thumbnailLinkList;

    public ViewerPagerAdapter(Context context, List<DriveFile> thumbnailLinkList) {
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.thumbnailLinkList = thumbnailLinkList;
    }

    @Override
    public int getCount() {
        return thumbnailLinkList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.pager_item, container, false);

        ImageView imageView = itemView.findViewById(R.id.imageView);
        String thumbnailLink = replaceThumbnailSize(thumbnailLinkList.get(position).getThumbnailLink());
        GlideApp.with(imageView.getContext())
                .load(thumbnailLink)
                .into(imageView);

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }

    public String replaceThumbnailSize(String string) {
        int pos = string.lastIndexOf("s550");
        if (pos > -1) {
            return string.substring(0, pos)
                    + "s3000"
                    + string.substring(pos + "s550".length(), string.length());
        } else {
            return string;
        }
    }
}

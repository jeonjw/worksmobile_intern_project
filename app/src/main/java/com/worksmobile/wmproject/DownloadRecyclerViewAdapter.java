package com.worksmobile.wmproject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.ArrayList;
import java.util.List;


public class DownloadRecyclerViewAdapter extends RecyclerView.Adapter<DownloadRecyclerViewAdapter.DownloadItemViewHolder> {
    private List<DriveFile> downloadItemList;

    public DownloadRecyclerViewAdapter(List<DriveFile> downloadItemList) {
        this.downloadItemList = downloadItemList;
    }

    @Override
    public DownloadItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item, parent, false);

        return new DownloadItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DownloadItemViewHolder holder, int position) {
        DriveFile file = downloadItemList.get(position);
        String thumbnailLink = replaceThumbnailSize(file.getThumbnailLink(), calculateProperThumbnailSize(file.getWidth(), file.getHeight()));

        GlideApp.with(holder.thumbnailImageView)
                .load(thumbnailLink)
                .centerCrop()
                .into(holder.thumbnailImageView);
        holder.fileNameTextView.setText(file.getName());
        holder.dateTextView.setText(file.getCreatedTime().toString());

    }

    private String calculateProperThumbnailSize(int width, int height) {
        String properWidth = String.valueOf(width * 2 / 10);
        String properHeight = String.valueOf(height * 2 / 10);
        return "w" + properWidth + "-" + "h" + properHeight;

    }

    public String replaceThumbnailSize(String string, String replacement) {
        int pos = string.lastIndexOf("s220");
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + "s220".length(), string.length());
        } else {
            return string;
        }
    }

    @Override
    public int getItemCount() {
        return downloadItemList.size();
    }

    class DownloadItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnailImageView;
        private TextView fileNameTextView;
        private TextView dateTextView;

        public DownloadItemViewHolder(View itemView) {
            super(itemView);

            thumbnailImageView = itemView.findViewById(R.id.download_thumbnail_imageview);
            fileNameTextView = itemView.findViewById(R.id.download_file_name_textview);
            dateTextView = itemView.findViewById(R.id.download_date_text_view);

        }
    }
}

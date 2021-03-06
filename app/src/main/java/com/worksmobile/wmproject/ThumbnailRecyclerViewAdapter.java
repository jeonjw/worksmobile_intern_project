package com.worksmobile.wmproject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.util.ImageUtil;
import com.worksmobile.wmproject.value_object.DriveFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThumbnailRecyclerViewAdapter extends RecyclerView.Adapter<ThumbnailRecyclerViewAdapter.ThumbnailViewHolder> {
    private List<DriveFile> fileList;
    private Set<Integer> checkedItems;
    private View.OnClickListener itemClickListener;
    private OnModeChangeListener modeChangeListener;
    private boolean isCheckBoxShowing;
    private boolean selectAll;

    public ThumbnailRecyclerViewAdapter(List<DriveFile> thumbnailLinkList, View.OnClickListener clickListener, OnModeChangeListener modeChangeListener) {
        this.fileList = thumbnailLinkList;
        this.itemClickListener = clickListener;
        this.modeChangeListener = modeChangeListener;
        checkedItems = new HashSet<>();
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.thumbnail_item, parent, false);
        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        DriveFile file = fileList.get(position);
        String mimeType = file.getMimeType();
        if (file.getThumbnailLink() != null) {
            if (mimeType.contains("image") || mimeType.contains("video")) {
                String thumbnailLink = ImageUtil.replaceThumbnailSize(file.getThumbnailLink(),
                        ImageUtil.calculateProperThumbnailSize(file.getWidth(), file.getHeight()));

                ImageUtil.loadImageWithSizeOverride(holder.imageView, thumbnailLink, file.getWidth() * 3 / 20, file.getHeight() * 3 / 20);
            } else {
                ImageUtil.loadImageWithUrl(holder.imageView, file.getThumbnailLink());
            }
        } else { //섬네일 링크가 없을 때
            int imageId = mimeType.contains("video") ? R.drawable.video_default : R.drawable.image_default;
            ImageUtil.loadImageWithResourceId(holder.imageView, imageId);
        }

        if (isCheckBoxShowing) {
            holder.checkBox.setVisibility(View.VISIBLE);
            if (checkedItems.contains(position))
                holder.checkBox.setChecked(true);
            else
                holder.checkBox.setChecked(false);
        } else {
            holder.checkBox.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void setSelectMode(boolean show) {
        isCheckBoxShowing = show;

        if (!show)
            clearCheckedItem();

        notifyDataSetChanged();
    }

    public void clearCheckedItem() {
        checkedItems.clear();
    }

    public ArrayList<DriveFile> getCheckedFileList() {
        ArrayList<DriveFile> checkedFileList = new ArrayList<>();
        for (Integer i : checkedItems) {
            checkedFileList.add(fileList.get(i));

        }
        return checkedFileList;
    }

    public void selectItem(int position) {
        if (checkedItems.contains(position))
            checkedItems.remove(position);
        else
            checkedItems.add(position);

        notifyItemChanged(position);
    }

    public void checkAllItems() {
        selectAll ^= true;
        if (selectAll) {
            for (int i = 0; i < fileList.size(); i++) {
                checkedItems.add(i);
            }
        } else {
            checkedItems.clear();
        }
        notifyDataSetChanged();
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        notifyDataSetChanged();
    }

    class ThumbnailViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private CheckBox checkBox;

        public ThumbnailViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail_viewholder_imageview);
            checkBox = itemView.findViewById(R.id.thumbnail_checkbox);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (checked) {
                        checkedItems.add(getAdapterPosition());
                    } else {
                        checkedItems.remove(getAdapterPosition());
                    }
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onClick(view);
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    checkBox.setChecked(true);
                    notifyDataSetChanged();
                    modeChangeListener.onChanged();
                    return true;
                }
            });
        }
    }
}

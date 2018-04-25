package com.worksmobile.wmproject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.worksmobile.wmproject.callback.OnModeChangeListener;
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
        String thumbnailLink = file.getThumbnailLink();
        if (mimeType.contains("image") || mimeType.contains("video") && file.getThumbnailLink() != null)
            thumbnailLink = replaceThumbnailSize(file.getThumbnailLink(), calculateProperThumbnailSize(file.getWidth(), file.getHeight()));

        GlideApp.with(holder.imageView)
                .load(thumbnailLink)
                .centerCrop()
                .into(holder.imageView);

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

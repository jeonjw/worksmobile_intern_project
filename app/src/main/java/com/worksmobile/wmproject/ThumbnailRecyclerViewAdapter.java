package com.worksmobile.wmproject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThumbnailRecyclerViewAdapter extends RecyclerView.Adapter<ThumbnailRecyclerViewAdapter.ThumbnailViewHolder> {
    private List<DriveFile> thumbnailLinkList;
    private Set<Integer> checkedItems;
    private View.OnClickListener itemClickListener;
    private OnModeChangeListener modeChangeListener;
    private boolean allCheckboxShow;

    public ThumbnailRecyclerViewAdapter(List<DriveFile> thumbnailLinkList, View.OnClickListener clickListener, OnModeChangeListener modeChangeListener) {
        this.thumbnailLinkList = thumbnailLinkList;
        this.itemClickListener = clickListener;
        this.modeChangeListener = modeChangeListener;
        checkedItems = new HashSet<>();
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.thumbnail_item, parent, false);
        view.setOnClickListener(itemClickListener);

        return new ThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {

        GlideApp.with(holder.imageView)
                .load(thumbnailLinkList.get(position).getThumbnailLink())
                .centerCrop()
                .into(holder.imageView);

        if (allCheckboxShow) {
            holder.checkBox.setVisibility(View.VISIBLE);
            if (checkedItems.contains(position))
                holder.checkBox.setChecked(true);
            else
                holder.checkBox.setChecked(false);
        } else {
            holder.checkBox.setVisibility(View.INVISIBLE);
        }
    }

    public void setSelectMode(boolean mode) {
        allCheckboxShow = mode;

        if (!mode)
            checkedItems.clear();

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return thumbnailLinkList.size();
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

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    allCheckboxShow = true;
                    checkBox.setChecked(true);
                    notifyDataSetChanged();
                    modeChangeListener.onSelectChanged(true);
                    return true;
                }
            });
        }
    }
}

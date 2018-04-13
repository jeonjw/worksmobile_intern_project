package com.worksmobile.wmproject;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.List;

public class ThumbnailRecyclerViewAdapter extends RecyclerView.Adapter<ThumbnailRecyclerViewAdapter.ThumbnailViewHolder> {
    private List<DriveFile> thumbnailLinkList;
    private View.OnClickListener itemClickListener;
    private boolean allCheckboxShow;

    public ThumbnailRecyclerViewAdapter(List<DriveFile> thumbnailLinkList, View.OnClickListener clickListener) {
        this.thumbnailLinkList = thumbnailLinkList;
        this.itemClickListener = clickListener;
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
            holder.checkBox.setChecked(thumbnailLinkList.get(position).isSelected());
        } else {
            holder.checkBox.setVisibility(View.INVISIBLE);
        }
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
                        thumbnailLinkList.get(getAdapterPosition()).setSelected(true);
                    } else {
                        thumbnailLinkList.get(getAdapterPosition()).setSelected(false);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    System.out.println("LONG CLICK");
                    allCheckboxShow = true;
                    checkBox.setChecked(true);
                    notifyDataSetChanged();
                    return true;
                }
            });
        }
    }
}

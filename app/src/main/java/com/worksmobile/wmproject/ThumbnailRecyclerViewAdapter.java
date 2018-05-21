package com.worksmobile.wmproject;

import android.databinding.ObservableList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.databinding.ThumbnailItemBinding;
import com.worksmobile.wmproject.value_object.DriveFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ThumbnailRecyclerViewAdapter extends RecyclerView.Adapter<ThumbnailRecyclerViewAdapter.ThumbnailViewHolder> {

    private List<DriveFile> fileList;
    private View.OnClickListener itemClickListener;
    private OnModeChangeListener modeChangeListener;
    private final WeakReferenceOnListChangedCallback onListChangedCallback;
    private boolean selectMode;
    private boolean selectAll;

    public ThumbnailRecyclerViewAdapter(View.OnClickListener clickListener, OnModeChangeListener modeChangeListener) {
        this.itemClickListener = clickListener;
        this.modeChangeListener = modeChangeListener;
        this.onListChangedCallback = new WeakReferenceOnListChangedCallback(this);
    }

    @Override
    public ThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ThumbnailItemBinding binding = ThumbnailItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ThumbnailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ThumbnailViewHolder holder, int position) {
        DriveFile file = fileList.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void setFileList(List<DriveFile> fileList) {
        this.fileList = fileList;
    }

    public void setSelectMode(boolean show) {
        selectMode = show;

        if (!show)
            clearCheckedItem();

        notifyDataSetChanged();
    }

    public void clearCheckedItem() {
        for (DriveFile file : fileList) {
            file.setChecked(false);
        }
    }

    public ArrayList<DriveFile> getCheckedFileList() {
        ArrayList<DriveFile> checkedFileList = new ArrayList<>();
        for (DriveFile file : fileList) {
            if (file.isChecked())
                checkedFileList.add(file);
        }
        return checkedFileList;
    }

    public void selectItem(int position) {
        DriveFile file = fileList.get(position);
        file.changeCheckedValue();
        notifyItemChanged(position);
    }

    public void checkAllItems() {
        selectAll ^= true;
        for (DriveFile file : fileList) {
            file.setChecked(selectAll);
        }
        notifyDataSetChanged();
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public boolean isSelectMode() {
        return selectMode;
    }

    class ThumbnailViewHolder extends RecyclerView.ViewHolder {

        private ThumbnailItemBinding binding;

        public ThumbnailViewHolder(ThumbnailItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onClick(view);
                }
            });

            binding.getRoot().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    binding.getDriveFile().changeCheckedValue();
                    modeChangeListener.onChanged();
                    return true;
                }
            });
        }

        public void bind(DriveFile driveFile) {
            binding.setDriveFile(driveFile);
        }
    }

    private static class WeakReferenceOnListChangedCallback extends ObservableList.OnListChangedCallback {

        private final WeakReference<ThumbnailRecyclerViewAdapter> adapterReference;

        public WeakReferenceOnListChangedCallback(ThumbnailRecyclerViewAdapter thumbnailRecyclerViewAdapter) {
            this.adapterReference = new WeakReference<>(thumbnailRecyclerViewAdapter);
        }

        @Override
        public void onChanged(ObservableList sender) {
            RecyclerView.Adapter adapter = adapterReference.get();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
            RecyclerView.Adapter adapter = adapterReference.get();
            if (adapter != null) {
                adapter.notifyItemRangeChanged(positionStart, itemCount);
            }
        }

        @Override
        public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
            RecyclerView.Adapter adapter = adapterReference.get();
            if (adapter != null) {
                adapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        @Override
        public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
            RecyclerView.Adapter adapter = adapterReference.get();
            if (adapter != null) {
                adapter.notifyItemMoved(fromPosition, toPosition);
            }
        }

        @Override
        public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
            RecyclerView.Adapter adapter = adapterReference.get();
            if (adapter != null) {
                adapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }
    }
}

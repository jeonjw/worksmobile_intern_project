package com.worksmobile.wmproject.ui;


import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.MainViewModel;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ThumbnailItemDecoration;
import com.worksmobile.wmproject.ThumbnailRecyclerViewAdapter;
import com.worksmobile.wmproject.callback.AdapterNavigator;
import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.databinding.FragmentBaseBinding;
import com.worksmobile.wmproject.value_object.DriveFile;

import java.util.ArrayList;
import java.util.Locale;

public abstract class BaseFragment extends Fragment
        implements OnSelectModeClickListener, AdapterNavigator {
    private static final String TAG = "BASE_FRAGMENT";

    protected MainViewModel mainViewModel;
    protected RecyclerView recyclerView;
    protected View.OnClickListener itemClickListener;
    protected ThumbnailRecyclerViewAdapter adapter;

    private View.OnClickListener selectModeClickListener;
    private OnModeChangeListener modeChangeListener;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentBaseBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_base, container, false);
        View view = binding.getRoot();
        setHasOptionsMenu(true);

        recyclerView = binding.thumbnailRecyclerview;

        initClickListener();
        adapter = new ThumbnailRecyclerViewAdapter(itemClickListener, modeChangeListener);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.addItemDecoration(new ThumbnailItemDecoration(3, 3));
        recyclerView.setAdapter(adapter);

        binding.setViewmodel(mainViewModel);
        mainViewModel.requestWholeList(isTrashFragment(), getMimeType());
        this.mainViewModel.setAdapterNavigator(this);

        return view;
    }

    public void setMainViewModel(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
        this.mainViewModel.currentSortingCriteria.set(R.id.taken_time_new);
    }

    public void initClickListener() {
        itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
                intent.putExtra("FILE_LIST", mainViewModel.fileList);
                intent.putExtra("VIEWER_POSITION", itemPosition);
                startActivity(intent);
            }
        };

        selectModeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                adapter.selectItem(itemPosition);
                changeSelectMode();
            }
        };

        modeChangeListener = new OnModeChangeListener() {
            @Override
            public void onChanged() {
                changeSelectMode();
            }
        };
    }

    private void changeSelectMode() {
        adapter.setItemClickListener(selectModeClickListener);
        ((MainActivity) getActivity()).changeToolbarSelectMode();
        mainViewModel.selectMode.set(true);
        getActivity().invalidateOptionsMenu();
    }

    private void changeViewerMode() {
        adapter.clearCheckedItem();
        adapter.setItemClickListener(itemClickListener);
        mainViewModel.selectMode.set(false);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_check_button:
                ((MainActivity) getActivity()).changeToolbarSelectMode();
                changeSelectMode();
                break;

            case R.id.toolbar_select_all:
                adapter.checkAllItems();
                break;
            case R.id.taken_time_new:
            case R.id.taken_time_old:
            case R.id.uploaded_time_new:
            case R.id.uploaded_time_old:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                mainViewModel.currentSortingCriteria.set(item.getItemId());
                break;

        }
        return true;
    }

    @Override
    public void onCancel() {
        changeViewerMode();
    }

    /**
     * ViewModel
     */
    @Override
    public void onDownload() {
        ArrayList<DriveFile> downloadList = adapter.getCheckedFileList();
        Intent intent = new Intent(getActivity(), DownloadActivity.class);
        intent.putExtra("DOWNLOAD_LIST", downloadList);
        startActivity(intent);
    }

    @Override
    public void onDelete() {
        createAlertDialog(adapter.getCheckedFileList().size());
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    private void createAlertDialog(int count) {
        String deleteMessage = String.format(Locale.KOREA, "%d개의 항목을 삭제하시겠습니까?\n삭제된 항목은 휴지통으로 이동합니다.", count);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle("삭제하기")
                .setMessage(deleteMessage)
                .setCancelable(false)
                .setPositiveButton("아니오",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("예",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mainViewModel.deleteFile(adapter.getCheckedFileList(),isTrashFragment());
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);
        menu.findItem(R.id.toolbar_check_button).setVisible(!mainViewModel.selectMode.get());
        menu.findItem(R.id.toolbar_select_all).setVisible(mainViewModel.selectMode.get());
    }

    public abstract String getMimeType();

    public abstract boolean isTrashFragment();

    @Override
    public void notifyChange() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void clearList() {
        adapter.clearCheckedItem();
    }
}

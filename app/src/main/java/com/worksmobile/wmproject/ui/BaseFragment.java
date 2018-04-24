package com.worksmobile.wmproject.ui;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ThumbnailItemDecoration;
import com.worksmobile.wmproject.ThumbnailRecyclerViewAdapter;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;
import com.worksmobile.wmproject.retrofit_object.MediaMetadata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public abstract class BaseFragment extends Fragment implements OnSelectModeClickListener {
    private static final int DELETE = 100;

    protected RecyclerView recyclerView;
    protected View.OnClickListener itemClickListener;
    protected ArrayList<DriveFile> fileList;

    private SwipeRefreshLayout swipeContainer;
    protected DriveHelper driveHelper;
    protected ThumbnailRecyclerViewAdapter adapter;
    private View.OnClickListener selectModeClickListener;
    private OnModeChangeListener modeChangeListener;
    private int currentSortingCriteria;
    private ProgressBar progressBar;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        setHasOptionsMenu(true);

        currentSortingCriteria = R.id.taken_time_new;
        driveHelper = new DriveHelper(getContext());
        fileList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.thumbnail_recyclerview);
        progressBar = view.findViewById(R.id.view_progress_bar);

        initClickListener();

        adapter = new ThumbnailRecyclerViewAdapter(fileList, itemClickListener, modeChangeListener);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.addItemDecoration(new ThumbnailItemDecoration(3, 3));
        recyclerView.setAdapter(adapter);

        swipeContainer = view.findViewById(R.id.pull_to_refresh);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWholeList();
            }
        });

        requestWholeList();

        return view;
    }

    public void initClickListener() {
        itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
                intent.putExtra("FILE_LIST", fileList);
                intent.putExtra("VIEWER_POSITION", itemPosition);
                startActivity(intent);
            }
        };

        selectModeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                adapter.selectItem(itemPosition);
            }
        };

        modeChangeListener = new OnModeChangeListener() {
            @Override
            public void onChanged() {
                ((MainActivity) getActivity()).changeToolbarSelectMode();
                chageSelectMode();
            }
        };
    }

    private void chageSelectMode() {
        adapter.setSelectMode(true);
        adapter.setItemClickListener(selectModeClickListener);
    }

    private void changeViewerMode() {
        adapter.setSelectMode(false);
        adapter.setItemClickListener(itemClickListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.toolbar_check_button:
                ((MainActivity) getActivity()).changeToolbarSelectMode();
                chageSelectMode();
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
                currentSortingCriteria = item.getItemId();
                sorting(item.getItemId());
                break;

        }
        return true;
    }

    private void requestWholeList() {
        progressBar.setVisibility(View.VISIBLE);
        driveHelper.enqueueListCreationCall(isTrashFragment(), getMimeType(), new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                adapter.clearCheckedItem();
                fileList.clear();
                fileList.addAll(Arrays.asList(driveFiles));
                sorting(currentSortingCriteria);
                adapter.notifyDataSetChanged();
                if (swipeContainer.isRefreshing())
                    swipeContainer.setRefreshing(false);

                progressBar.setVisibility(View.GONE);


                /**
                 이미지 파일의 위도 경도가 존재한다면 추후 위도 경도 쿼리를 진행하기 위해 properties Update과정을 거친다. (마이그레이션 코드 추후 삭제할것)
                 조건문 변경해야함, 프로퍼티가 없고, 로케이션이 있을 때 마이그레이션 진행하도록 변경해야함 (4.24 PM 07)
                 */
                for (DriveFile file : driveFiles) {
                    MediaMetadata mediaMetadata = file.getImageMediaMetadata();
                    if (mediaMetadata != null) {
                        if (mediaMetadata.getLocation() != null)
                            driveHelper.setLocationProperties(file, null);
                    }
                }
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void sorting(int sortBy) {
        switch (sortBy) {
            case R.id.taken_time_new:
                Collections.sort(fileList, (o1, o2) -> {
                    Date date1 = getValidTakenTime(o1.getTakenTime(), o1.getCreatedTime());
                    Date date2 = getValidTakenTime(o2.getTakenTime(), o2.getCreatedTime());
                    return date2.compareTo(date1);
                });
                break;
            case R.id.taken_time_old:
                Collections.sort(fileList, (o1, o2) -> {
                    Date date1 = getValidTakenTime(o1.getTakenTime(), o1.getCreatedTime());
                    Date date2 = getValidTakenTime(o2.getTakenTime(), o2.getCreatedTime());
                    return date1.compareTo(date2);
                });
                break;
            case R.id.uploaded_time_new:
                Collections.sort(fileList, (o1, o2) -> {
                    if (o1.getCreatedTime() == null || o2.getCreatedTime() == null)
                        return 0;
                    return o2.getCreatedTime().compareTo(o1.getCreatedTime());
                });
                break;
            case R.id.uploaded_time_old:
                Collections.sort(fileList, (o1, o2) -> {
                    if (o1.getCreatedTime() == null || o2.getCreatedTime() == null)
                        return 0;
                    return o1.getCreatedTime().compareTo(o2.getCreatedTime());
                });
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private Date getValidTakenTime(String takenTime, Date createdTime) {
        Date date = null;
        try {
            if (takenTime == null)
                date = createdTime;
            else {
                date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.KOREA).parse(takenTime);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }


    @Override
    public void onCancel() {
        changeViewerMode();
    }

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

    public void requestDelete() {
        for (DriveFile file : adapter.getCheckedFileList()) {
            driveHelper.enqueueDeleteCall(file, new StateCallback() {
                @Override
                public void onSuccess(String msg) {
                    if (msg == null) {
                        int position = fileList.indexOf(file);
                        fileList.remove(position);
                        adapter.notifyItemRemoved(position);
                    }

                }

                @Override
                public void onFailure(String msg) {

                }
            });
        }
        adapter.clearCheckedItem();
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
                                requestDelete();
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public abstract String getMimeType();

    public abstract boolean isTrashFragment();


}

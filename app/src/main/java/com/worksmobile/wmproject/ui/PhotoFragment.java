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
import android.widget.Toast;

import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.ThumbnailItemDecoration;
import com.worksmobile.wmproject.ThumbnailRecyclerViewAdapter;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.OnModeChangeListener;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

import java.util.ArrayList;
import java.util.Locale;

public class PhotoFragment extends Fragment
        implements OnSelectModeClickListener {


    private static final int DELETE = 100;
    private static final int DOWNLOAD = 101;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeContainer;
    private DriveHelper driveHelper;
    private ArrayList<DriveFile> fileList;
    private ThumbnailRecyclerViewAdapter adapter;

    private View.OnClickListener onClickListener;
    private OnModeChangeListener onModeChangeListener;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        setHasOptionsMenu(true);

        driveHelper = new DriveHelper(getContext());
        fileList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.thumbnail_recyclerview);
        initClickListener();

        adapter = new ThumbnailRecyclerViewAdapter(fileList, onClickListener, onModeChangeListener);
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

    private void initClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                intent.putExtra("FILE_LIST", fileList);
                intent.putExtra("VIEWER_POSITION", itemPosition);
                startActivity(intent);
            }
        };
        onModeChangeListener = new OnModeChangeListener() {
            @Override
            public void onSelectChanged(boolean checked) {
                ((MainActivity) getActivity()).changeToolbarSelectMode();
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.toolbar_check_button) {
            Toast.makeText(getContext(), "Checked", Toast.LENGTH_SHORT).show();
            ((MainActivity) getActivity()).changeToolbarSelectMode();
            adapter.setSelectMode(true);
        }
        return true;
    }

    private void requestWholeList() {
        driveHelper.enqueueListCreationCall("root", new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                fileList.clear();
                for (DriveFile file : driveFiles) {
                    String thumbnailLink = file.getThumbnailLink();
                    file.setThumbnailLink(replaceThumbnailSize(thumbnailLink, "s220", "s550"));
                    fileList.add(file);
                }
                adapter.notifyDataSetChanged();
                if (swipeContainer.isRefreshing())
                    swipeContainer.setRefreshing(false);
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    public String replaceThumbnailSize(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }

    @Override
    public void onCancel() {
        adapter.setSelectMode(false);
    }

    @Override
    public void onDownload() {

    }

    @Override
    public void onDelete() {
        createAlertDialog(DELETE, adapter.getCheckedFileList().size());
    }

    public void requestDelete() {
        for (DriveFile file : adapter.getCheckedFileList()) {
            driveHelper.enqueueFileDeleteCall(file.getId(), new StateCallback() {
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

    private void createAlertDialog(int command, int count) {
        String deleteMessage = String.format(Locale.KOREA, "%d개의 항목을 삭제하시겠습니까?\n삭제된 항목은 휴지통으로 이동합니다.", count);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
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
                                if (command == DELETE)
                                    requestDelete();
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
}

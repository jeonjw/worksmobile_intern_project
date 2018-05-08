package com.worksmobile.wmproject.ui;


import android.view.View;

import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.value_object.DriveFile;

public class TrashFragment extends BaseFragment {
    private static final String MIME_TYPE = null;
    private int deleteCount = 0;

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public boolean isTrashFragment() {
        return true;
    }

    @Override
    public void requestDelete() {
        deleteCount = 0;
        int totalDeleteCount = adapter.getCheckedFileList().size();
        progressBar.setVisibility(View.VISIBLE);

        for (DriveFile file : adapter.getCheckedFileList()) {
            driveHelper.enqueuePermanentDeleteCall(file.getId(), new StateCallback() {
                @Override
                public void onSuccess(String msg) {
                    if (msg == null) {
                        deleteCount++;
                        int position = fileList.indexOf(file);
                        fileList.remove(position);

                        if (deleteCount == totalDeleteCount) {
                            progressBar.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                            adapter.clearCheckedItem();
                        }
                    }
                }

                @Override
                public void onFailure(String msg) {

                }
            });
        }
        adapter.clearCheckedItem();
    }
}

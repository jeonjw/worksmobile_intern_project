package com.worksmobile.wmproject.ui;


import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.retrofit_object.DriveFile;

public class TrashFragment extends BaseFragment {
    private static final String MIME_TYPE = null;

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
        for (DriveFile file : adapter.getCheckedFileList()) {
            driveHelper.enqueuePermanentDeleteCall(file.getId(), new StateCallback() {
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
}

package com.worksmobile.wmproject.ui;


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
}

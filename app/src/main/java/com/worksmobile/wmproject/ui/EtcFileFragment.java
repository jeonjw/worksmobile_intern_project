package com.worksmobile.wmproject.ui;

import android.view.View;

public class EtcFileFragment extends BaseFragment {

    private static final String MIME_TYPE = "application/";

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public void initClickListener() {
        super.initClickListener();
        itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        };
    }

    @Override
    public boolean isTrashFragment() {
        return false;
    }
}

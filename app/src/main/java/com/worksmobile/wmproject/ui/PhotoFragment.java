package com.worksmobile.wmproject.ui;

import android.content.Intent;
import android.view.View;

public class PhotoFragment extends BaseFragment {
    private static final String MIME_TYPE = "image/";

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public boolean isTrashFragment() {
        return false;
    }

    @Override
    public void initClickListener() {
        super.initClickListener();
        itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemPosition = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                intent.putExtra("FILE_LIST", mainViewModel.fileList);
                intent.putExtra("VIEWER_POSITION", itemPosition);
                startActivity(intent);
            }
        };
    }
}

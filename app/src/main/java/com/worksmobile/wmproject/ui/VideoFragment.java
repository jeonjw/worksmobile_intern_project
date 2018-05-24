package com.worksmobile.wmproject.ui;

import android.content.Intent;
import android.view.View;


public class VideoFragment extends BaseFragment {
    private static final String MIME_TYPE = "video/";

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
                Intent intent = new Intent(getContext(), VideoViewerActivity.class);
                intent.putExtra("FILE_NAME", mainViewModel.fileList.get(itemPosition).getName());
                startActivity(intent);
            }
        };
    }


}

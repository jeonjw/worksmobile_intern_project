package com.worksmobile.wmproject;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ThumbnailItemDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;

    public ThumbnailItemDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int column = position % spanCount;

        outRect.left = spacing - column * spacing / spanCount;
        outRect.right = (column + 1) * spacing / spanCount;

        if (position < spanCount) { // top edge
            outRect.top = spacing;
        }
        outRect.bottom = spacing; // item bottom
    }
}
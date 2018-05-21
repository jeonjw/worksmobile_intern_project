package com.worksmobile.wmproject;

import android.databinding.BindingConversion;
import android.view.View;

public class BindingConversions {
    @BindingConversion
    public static int convertBooleanToVisibility(boolean visible) {
        return visible ? View.VISIBLE : View.INVISIBLE;
    }
}

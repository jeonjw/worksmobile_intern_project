package com.worksmobile.wmproject.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.value_object.DriveFile;
import com.worksmobile.wmproject.value_object.MediaMetadata;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class FileDetailInfoFragment extends Fragment {
    private static final String TAG = "FILE_DETAIL_INFO";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_detail_info, container, false);
        TextView resolustionTextView = view.findViewById(R.id.resolution_value_textview);
        TextView sizeTextView = view.findViewById(R.id.size_value_textview);
        TextView uploadDateTextView = view.findViewById(R.id.upload_date_value_textview);

        DriveFile file = null;

        if (getArguments() != null) {
            file = (DriveFile) getArguments().getSerializable("DRIVE_FILE");
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
        String tempDate = dateFormat.format(file.getCreatedTime());


        resolustionTextView.setText(file.getHeight() + " x  " + file.getWidth());
        sizeTextView.setText(String.format("%.1fMB", file.getSize() / 1000000f));
        uploadDateTextView.setText(tempDate);

        if (file.getImageMediaMetadata().getCameraModel() == null) {
            view.findViewById(R.id.group).setVisibility(View.GONE);
            view.findViewById(R.id.no_taken_info).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.group).setVisibility(View.VISIBLE);
            view.findViewById(R.id.no_taken_info).setVisibility(View.GONE);
            MediaMetadata mediaMetadata = file.getImageMediaMetadata();
            ((TextView) view.findViewById(R.id.taken_time_value_textview)).setText(mediaMetadata.getTime());
            ((TextView) view.findViewById(R.id.camera_value_textview)).setText(mediaMetadata.getCameraModel());
            ((TextView) view.findViewById(R.id.focal_length_value_textview)).setText(String.valueOf(mediaMetadata.getFocalLength()));
            ((TextView) view.findViewById(R.id.ISO_value_textview)).setText(String.valueOf(mediaMetadata.getIsoSpeed()));

        }


        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("TEST TOUCH");
                getActivity().getSupportFragmentManager().popBackStack();
                return true;
            }
        });
        return view;
    }
}

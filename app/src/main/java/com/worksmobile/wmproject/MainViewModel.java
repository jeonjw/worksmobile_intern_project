package com.worksmobile.wmproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.BaseObservable;
import android.databinding.Observable;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.worksmobile.wmproject.callback.AdapterNavigator;
import com.worksmobile.wmproject.callback.ListCallback;
import com.worksmobile.wmproject.callback.StateCallback;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.service.MediaStoreJobService;
import com.worksmobile.wmproject.service.MediaStoreService;
import com.worksmobile.wmproject.util.FileUtils;
import com.worksmobile.wmproject.value_object.DriveFile;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainViewModel extends BaseObservable {

    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 777;
    private static final int FILE_SELECT_CODE = 888;

    private Context context;
    private DriveHelper driveHelper;

    public final ObservableField<String> currentToolbarTitle = new ObservableField<>();
    public final ObservableBoolean selectMode = new ObservableBoolean(false);
    public final ObservableBoolean loading = new ObservableBoolean(false);
    public final ObservableField<String> snackbarText = new ObservableField<>();
    public final ObservableArrayList<DriveFile> fileList = new ObservableArrayList<>();
    public final ObservableInt currentSortingCriteria = new ObservableInt();

    private AdapterNavigator adapterNavigator;
    private boolean lastTrashFragmentValue;
    private String lastMimeType;
    private int deleteCount;


    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    public MainViewModel(Context context) {
        this.context = context;
        driveHelper = new DriveHelper(context);
        currentSortingCriteria.addOnPropertyChangedCallback(new OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (adapterNavigator != null)
                    sortList(currentSortingCriteria.get());
            }
        });
    }

    public void checkPermission() {
        if (!hasPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) context, PERMISSIONS, EXTERNAL_STORAGE_PERMISSION_CODE);
        } else {
            setJobSchedule();
        }
    }

    private boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(context, uri);
                    if (path != null) {
                        DateFormat sdFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
                        Date nowDate = new Date();
                        String tempDate = sdFormat.format(nowDate);
                        AppDatabase.getDatabase(context).fileDAO().insertFileStatus(new FileStatus(path, tempDate, "UPLOAD"));
                        sendNewMediaBroadCast();
                        snackbarText.set("선택한 파일 업로드 요청");
                    } else {
                        snackbarText.set("가져올 수 없는 파일 입니다.");
                    }
                }
                break;
        }
    }

    public void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setJobSchedule();
                }
                break;
        }
    }

    public void setJobSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaStoreJobService.scheduleJob(context);
        } else {
            Intent mediaStoreService = new Intent(context, MediaStoreService.class);
            context.startService(mediaStoreService);
        }
    }

    public void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
    }

    private void sendNewMediaBroadCast() {
        Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
        intent.setClass(context, MyBroadcastReceiver.class);
        context.sendBroadcast(intent);
    }

    public void requestWholeList(boolean isTrashFragment, String mimeType) {
        lastTrashFragmentValue = isTrashFragment;
        lastMimeType = mimeType;
        loading.set(true);
        driveHelper.enqueueListCreationCall(isTrashFragment, mimeType, new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                adapterNavigator.clearList();
                fileList.clear();
                fileList.addAll(Arrays.asList(driveFiles));
                sortList(currentSortingCriteria.get());
                loading.set(false);
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    public void requestWholeList() {
        loading.set(true);
        driveHelper.enqueueListCreationCall(lastTrashFragmentValue, lastMimeType, new ListCallback() {
            @Override
            public void onSuccess(DriveFile[] driveFiles) {
                adapterNavigator.clearList();
                fileList.clear();
                fileList.addAll(Arrays.asList(driveFiles));
                sortList(currentSortingCriteria.get());
                loading.set(false);
            }

            @Override
            public void onFailure(String msg) {

            }
        });
    }

    private void sortList(int sortBy) {
        switch (sortBy) {
            case R.id.taken_time_new:
                Collections.sort(fileList, (o1, o2) -> {
                    Date date1 = getValidTakenTime(o1.getTakenTime(), o1.getCreatedTime());
                    Date date2 = getValidTakenTime(o2.getTakenTime(), o2.getCreatedTime());
                    return date2.compareTo(date1);
                });
                break;
            case R.id.taken_time_old:
                Collections.sort(fileList, (o1, o2) -> {
                    Date date1 = getValidTakenTime(o1.getTakenTime(), o1.getCreatedTime());
                    Date date2 = getValidTakenTime(o2.getTakenTime(), o2.getCreatedTime());
                    return date1.compareTo(date2);
                });
                break;
            case R.id.uploaded_time_new:
                Collections.sort(fileList, (o1, o2) -> {
                    if (o1.getCreatedTime() == null || o2.getCreatedTime() == null)
                        return 0;
                    return o2.getCreatedTime().compareTo(o1.getCreatedTime());
                });
                break;
            case R.id.uploaded_time_old:
                Collections.sort(fileList, (o1, o2) -> {
                    if (o1.getCreatedTime() == null || o2.getCreatedTime() == null)
                        return 0;
                    return o1.getCreatedTime().compareTo(o2.getCreatedTime());
                });
                break;
        }
        adapterNavigator.notifyChange();
    }

    private Date getValidTakenTime(String takenTime, Date createdTime) {
        Date date = null;
        try {
            if (takenTime == null)
                date = createdTime;
            else {
                if (takenTime.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                    date = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.KOREA).parse(takenTime);
                } else if (takenTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                    date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).parse(takenTime);
                }

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public void setAdapterNavigator(AdapterNavigator adapterNavigator) {
        this.adapterNavigator = adapterNavigator;
    }

    public void deleteFile(List<DriveFile> checkedList, boolean trashFragment) {
        deleteCount = 0;
        int totalDeleteCount = checkedList.size();
        loading.set(true);
        if (!trashFragment) {
            for (DriveFile file : checkedList) {
                driveHelper.enqueueDeleteCall(file, new StateCallback() {

                    @Override
                    public void onSuccess(String msg) {
                        if (msg == null) {
                            deleteCount++;
                            int position = fileList.indexOf(file);
                            fileList.remove(position);
                            if (deleteCount == totalDeleteCount) {
                                loading.set(false);
                                adapterNavigator.clearList();
                            }
                        }
                    }

                    @Override
                    public void onFailure(String msg) {
                    }
                });
            }
        } else {
            for (DriveFile file : checkedList) {
                driveHelper.enqueuePermanentDeleteCall(file.getId(), new StateCallback() {

                    @Override
                    public void onSuccess(String msg) {
                        if (msg == null) {
                            deleteCount++;
                            int position = fileList.indexOf(file);
                            fileList.remove(position);

                            if (deleteCount == totalDeleteCount) {
                                loading.set(false);
                                adapterNavigator.clearList();
                            }
                        }
                    }

                    @Override
                    public void onFailure(String msg) {
                    }
                });
            }
        }
    }

}

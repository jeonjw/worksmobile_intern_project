package com.worksmobile.wmproject.ui;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.worksmobile.wmproject.MyBroadCastReceiver;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.room.AppDatabase;
import com.worksmobile.wmproject.room.FileStatus;
import com.worksmobile.wmproject.service.MediaStoreJobService;
import com.worksmobile.wmproject.service.MediaStoreService;
import com.worksmobile.wmproject.util.FileUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 777;
    private static final int FILE_SELECT_CODE = 888;

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView toolbarTextView;
    private ActionBarDrawerToggle toggle;
    private OnSelectModeClickListener onSelectModeClickListener;
    private LinearLayout bottomView;
    private boolean selectMode;
    private MenuItem selectModeMenuItem;
    private MenuItem selectAllMenuItem;
    private FloatingActionButton floatingActionButton;
    private String currentToolbarTitle;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private View.OnClickListener drawerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        }
    };

    private View.OnClickListener selectModeCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSelectMode();
        }
    };

    private void closeSelectMode() {
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(drawerListener);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorNaverGreen));
        toolbarTextView.setText(currentToolbarTitle);

        if (onSelectModeClickListener != null)
            onSelectModeClickListener.onCancel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorNaverGreenDark));
        }

        selectMode = false;
        selectModeMenuItem.setVisible(true);
        selectAllMenuItem.setVisible(false);
        bottomView.setVisibility(View.GONE);
        floatingActionButton.setVisibility(View.VISIBLE);
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        checkPermission();

        toolbar = findViewById(R.id.main_toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomView = findViewById(R.id.bottom_view);
        floatingActionButton = findViewById(R.id.upload_floating_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });

        bottomView.findViewById(R.id.bottom_download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectModeClickListener.onDownload();
            }
        });

        bottomView.findViewById(R.id.bottom_delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectModeClickListener.onDelete();
            }
        });

        initDrawerToggle();
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        Fragment fragment = new PhotoFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.content_fragment, fragment).commit();
        onSelectModeClickListener = (PhotoFragment) fragment;

    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);

    }

    private void initDrawerToggle() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbarTextView = toolbar.findViewById(R.id.toolbar_text_view);
        toolbarTextView.setText(R.string.all_photo);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_start, R.string.drawer_end);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(drawerListener);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel("WM_PROJECT", "project1", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("channel description");
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }


    private void setJobSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaStoreJobService.scheduleJob(this);
        } else {
            final Intent intent = new Intent(MainActivity.this, MediaStoreService.class);
            startService(intent);
        }
    }


    private void checkPermission() {
        if (!hasPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, EXTERNAL_STORAGE_PERMISSION_CODE);
        } else {
            setJobSchedule();
        }
    }

    public boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setJobSchedule();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    if (path != null) {
                        DateFormat sdFormat = new SimpleDateFormat("yyyy. MM. dd HH:mm", Locale.KOREA);
                        Date nowDate = new Date();
                        String tempDate = sdFormat.format(nowDate);
                        AppDatabase.getDatabase(this).fileDAO().insertFileStatus(new FileStatus(path, tempDate, "UPLOAD"));

                        sendNewMediaBroadCast();
                    } else {
                        Snackbar.make(bottomView, "가져올 수 없는 파일 입니다.", Snackbar.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    private void sendNewMediaBroadCast() {
        Intent intent = new Intent("com.worksmobile.wm_project.NEW_MEDIA");
        intent.setClass(this, MyBroadCastReceiver.class);
        sendBroadcast(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        switch (id) {
            case R.id.nav_photo:
                fragment = new PhotoFragment();
                currentToolbarTitle = getString(R.string.all_photo);
                toolbarTextView.setText(currentToolbarTitle);
                break;
            case R.id.nav_photo_map:
                fragment = new MapFragment();
                currentToolbarTitle = "사진 지도";
                toolbarTextView.setText(currentToolbarTitle);
                drawerLayout.closeDrawer(GravityCompat.START);
                getSupportFragmentManager().beginTransaction().replace(R.id.content_fragment, fragment).commit();
                return true;
            case R.id.nav_video:
                fragment = new VideoFragment();
                currentToolbarTitle = getString(R.string.all_video);
                toolbarTextView.setText(currentToolbarTitle);
                break;

            case R.id.nav_document:
                fragment = new EtcFileFragment();
                currentToolbarTitle = getString(R.string.etc_file);
                toolbarTextView.setText(currentToolbarTitle);
                break;

            case R.id.nav_trash:
                fragment = new TrashFragment();
                currentToolbarTitle = getString(R.string.trash_can);
                toolbarTextView.setText(currentToolbarTitle);
                break;

        }

        drawerLayout.closeDrawer(GravityCompat.START);
        if (fragment != null) {
            onSelectModeClickListener = (BaseFragment) fragment;
            getSupportFragmentManager().beginTransaction().replace(R.id.content_fragment, fragment).commit();
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        selectModeMenuItem = menu.findItem(R.id.toolbar_check_button);
        selectAllMenuItem = menu.findItem(R.id.toolbar_select_all);
        return true;
    }

    public void changeToolbarSelectMode() {
        if (selectMode)
            return;

        toggle.setHomeAsUpIndicator(R.drawable.ic_close);
        toolbar.setNavigationOnClickListener(selectModeCloseListener);
        toolbarTextView.setText(R.string.choose_photo);

        toolbar.setBackgroundColor(getResources().getColor(R.color.colorDarkGray));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorDarkGrayDark));
        }

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        bottomView.setVisibility(View.VISIBLE);
        bottomView.startAnimation(slideUp);
        selectModeMenuItem.setVisible(false);
        selectAllMenuItem.setVisible(true);
        floatingActionButton.setVisibility(View.GONE);
        selectMode = true;

    }

    @Override
    public void onBackPressed() {
        if (selectMode) {
            closeSelectMode();
        } else {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
    }
}

package com.worksmobile.wmproject.ui;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.service.MediaStoreJobService;
import com.worksmobile.wmproject.service.MediaStoreService;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 777;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView toolbarTextView;
    private ActionBarDrawerToggle toggle;
    private OnSelectModeClickListener onSelectModeClickListener;
    private LinearLayout bottomView;
    private boolean selectMode;
    private MenuItem selectModeMenuItem;

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
        toolbarTextView.setText("모든 사진");

        if (onSelectModeClickListener != null)
            onSelectModeClickListener.onCancel();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorNaverGreenDark));
        }

        bottomView.setVisibility(View.GONE);
        selectMode = false;
        selectModeMenuItem.setVisible(true);
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        checkPermission();

        toolbar = findViewById(R.id.main_toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomView = findViewById(R.id.bottom_view);

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

    private void initDrawerToggle() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbarTextView = toolbar.findViewById(R.id.toolbar_text_view);
        toolbarTextView.setText("모든 사진");

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                setJobSchedule();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "권한 허용", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
            }

        } else {
            setJobSchedule();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setJobSchedule();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.nav_photo:
                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_video:
                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_document:
                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_folder:
                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                break;

        }

        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        selectModeMenuItem = menu.findItem(R.id.toolbar_check_button);
        return true;
    }

    public void changeToolbarSelectMode() {
        toggle.setHomeAsUpIndicator(R.drawable.ic_close);
        toolbar.setNavigationOnClickListener(selectModeCloseListener);
        toolbarTextView.setText("사진을 선택하세요");

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

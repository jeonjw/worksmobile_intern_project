package com.worksmobile.wmproject.ui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.worksmobile.wmproject.MainViewModel;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.OnSelectModeClickListener;
import com.worksmobile.wmproject.databinding.ActivityMainBinding;

import java.util.Objects;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MAIN_ACTIVITY";

    private MainViewModel mainViewModel;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private OnSelectModeClickListener onSelectModeClickListener;
    private LinearLayout bottomView;
    private ActivityMainBinding binding;

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

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainViewModel = new MainViewModel(this);
        binding.setViewmodel(mainViewModel);
        createNotificationChannel();
        setupToolbar();
        setupNavigationDrawer();
        setupBottomView();


        mainViewModel.checkPermission();

        BaseFragment fragment = new PhotoFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.content_fragment, fragment).commit();
        onSelectModeClickListener = fragment;
        fragment.setMainViewModel(mainViewModel);

        binding.uploadFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainViewModel.showFileChooser();
            }
        });

        mainViewModel.snackbarText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showSnackbar();
            }
        });
    }

    private void setupToolbar() {
        toolbar = binding.mainToolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mainViewModel.currentToolbarTitle.set(getString(R.string.all_photo));
    }

    private void setupNavigationDrawer() {
        drawerLayout = binding.drawerLayout;
        NavigationView navigationView = binding.navigationView;
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_start, R.string.drawer_end);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(drawerListener);
    }

    private void setupBottomView() {
        bottomView = binding.bottomView;
        binding.bottomDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectModeClickListener.onDownload();
            }
        });

        binding.bottomDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectModeClickListener.onDelete();
            }
        });
    }

    private void showSnackbar() {
        Snackbar.make(binding.getRoot(), Objects.requireNonNull(mainViewModel.snackbarText.get()), Snackbar.LENGTH_SHORT).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mainViewModel.handlePermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mainViewModel.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        switch (id) {
            case R.id.nav_photo:
                fragment = new PhotoFragment();
                mainViewModel.currentToolbarTitle.set(getString(R.string.all_photo));
                break;
            case R.id.nav_photo_map:
                mainViewModel.currentToolbarTitle.set(getString(R.string.photo_map));
                drawerLayout.closeDrawer(GravityCompat.START);
                getSupportFragmentManager().beginTransaction().replace(R.id.content_fragment, new MapFragment()).commit();
                return true;
            case R.id.nav_video:
                fragment = new VideoFragment();
                mainViewModel.currentToolbarTitle.set(getString(R.string.all_video));
                break;

            case R.id.nav_document:
                fragment = new EtcFileFragment();
                mainViewModel.currentToolbarTitle.set(getString(R.string.etc_file));

                break;

            case R.id.nav_trash:
                fragment = new TrashFragment();
                mainViewModel.currentToolbarTitle.set(getString(R.string.trash_can));
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        if (fragment != null) {
            onSelectModeClickListener = (BaseFragment) fragment;
            ((BaseFragment) fragment).setMainViewModel(mainViewModel);
            getSupportFragmentManager().beginTransaction().replace(R.id.content_fragment, fragment).commit();
        }

        return true;
    }

    public void changeToolbarSelectMode() {
        if (mainViewModel.selectMode.get())
            return;

        toggle.setHomeAsUpIndicator(R.drawable.ic_close);
        toolbar.setNavigationOnClickListener(selectModeCloseListener);
        binding.toolbarTextView.setText(getString(R.string.choose_photo));

        toolbar.setBackgroundColor(getResources().getColor(R.color.colorDarkGray));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorDarkGrayDark));
        }

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        bottomView.startAnimation(slideUp);
    }

    public void closeSelectMode() {
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(drawerListener);
        toolbar.setBackgroundColor(getResources().getColor(R.color.colorNaverGreen));

        binding.toolbarTextView.setText(mainViewModel.currentToolbarTitle.get());

        if (onSelectModeClickListener != null)
            onSelectModeClickListener.onCancel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.colorNaverGreenDark));
        }
    }

    @Override
    public void onBackPressed() {
        if (mainViewModel.selectMode.get()) {
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

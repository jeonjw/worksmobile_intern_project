package com.worksmobile.wmproject.ui;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.worksmobile.wmproject.DriveHelper;
import com.worksmobile.wmproject.R;
import com.worksmobile.wmproject.callback.TokenCallback;
import com.worksmobile.wmproject.databinding.ActivityLoginBinding;
import com.worksmobile.wmproject.value_object.Token;

public class LoginActivity extends AppCompatActivity {

    private static final String SHARED_PREFERENCES_NAME = "TokenStatePreference";
    private static final String TOKEN_STATE = "TOKEN_STATE";
    private static final String USED_INTENT = "USED_INTENT";
    private static final String TAG = "LOGIN_ACTIVITY";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.setActivity(this);

        if (restoreAuthState() != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void login() {
        String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + getString(R.string.client_id) + "&" +
                "response_type=code&" +
                "access_type=offline&" +
                "prompt=consent&" +
                "scope=https://www.googleapis.com/auth/drive&" +
                "redirect_uri=com.worksmobile.wmproject:/oauth2callback";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL));
        startActivity(intent);
        finish();
    }

    private void getAuthCode() {
        Uri uri = getIntent().getData();
        if (uri != null)
            System.out.println("URI : " + uri.toString());
        if (uri != null && uri.toString().startsWith("com.worksmobile.wmproject:/oauth2callback")) {
            // use the parameter your API exposes for the code (mostly it's "code")
            String code = uri.getQueryParameter("code");
            if (code != null) {
                requestToken(code);
            } else if (uri.getQueryParameter("error") != null) {
                // show an error message here
            }
        }
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Intent.ACTION_VIEW:
                        if (!intent.hasExtra(USED_INTENT)) {
                            getAuthCode();
                            intent.putExtra(USED_INTENT, true);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void requestToken(String code) {
        DriveHelper driveHelper = new DriveHelper(this);
        driveHelper.enqueueToeknRequestCall(new TokenCallback() {
            @Override
            public void onSuccess(Token token) {
                if (token != null) {
                    persistToken(token);

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(String msg) {
                Log.e(TAG, msg);
            }
        }, code);

    }


    private void persistToken(@NonNull Token token) {
        Gson gson = new Gson();
        String tokenJson = gson.toJson(token);

        getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(TOKEN_STATE, tokenJson)
                .apply();
    }

    @Nullable
    private Token restoreAuthState() {
        Gson gson = new Gson();
        String jsonString = getSharedPreferences("TokenStatePreference", Context.MODE_PRIVATE).getString("TOKEN_STATE", null);
        return gson.fromJson(jsonString, Token.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

}

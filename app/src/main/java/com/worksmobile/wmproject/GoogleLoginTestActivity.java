package com.worksmobile.wmproject;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class GoogleLoginTestActivity extends AppCompatActivity {

    public static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com&" +
            "response_type=code&" +
            "access_type=offline&" +
            "prompt=consent&" +
            "scope=https://www.googleapis.com/auth/drive&" +
            "redirect_uri=com.worksmobile.wmproject:/oauth2callback";

    private static final String USED_INTENT = "USED_INTENT";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive);

        findViewById(R.id.google_sign_in_button).setOnClickListener(view -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(AUTH_URL));

            startActivity(intent);
        });
    }

    private void getAuthCode() {
        Uri uri = getIntent().getData();
        if (uri != null)
            System.out.println("URI : " + uri.toString());
        if (uri != null && uri.toString().startsWith("com.worksmobile.wmproject:/oauth2callback")) {
            // use the parameter your API exposes for the code (mostly it's "code")
            String code = uri.getQueryParameter("code");
            if (code != null) {
                System.out.println("TEST: " + code);
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

    @Override
    protected void onStart() {
        System.out.println("Onstart");
        super.onStart();
        checkIntent(getIntent());
    }

    private void requestToken(String code) {
        DriveHelper driveHelper = new DriveHelper("764478534049-ju7pr2csrhjr88sf111p60tl57g4bp3p.apps.googleusercontent.com", null);
        driveHelper.getToken(null, code);
    }


}

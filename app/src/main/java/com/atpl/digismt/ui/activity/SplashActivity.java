package com.atpl.digismt.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.atpl.digismt.R;

public class SplashActivity extends Activity {

    //instance of textview to show version information
    TextView versionInforamtion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        versionInforamtion = (TextView) findViewById(R.id.tv_version);

        try {

            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            String version = info.versionName;


            versionInforamtion.setText("Version:" + version);


            getActionBar().hide();

            try {


            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        scheduleSplashScreen();

    }


    private void scheduleSplashScreen() {
        int splashScreenDuration = 1000 * 4;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }, splashScreenDuration
        );
    }
}

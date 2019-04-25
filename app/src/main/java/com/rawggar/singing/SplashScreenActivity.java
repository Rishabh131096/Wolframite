package com.rawggar.singing;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import com.rawggar.singing.MainActivity;
import com.rawggar.singing.R;

public class SplashScreenActivity extends Activity {

    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
                overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
                finish();
            }
        },1000);

    }
}

package com.rawggar.singing;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;

import co.mobiwise.library.InteractivePlayerView;
import co.mobiwise.library.OnActionClickedListener;

public class PlayerActivity extends Activity implements OnActionClickedListener {

    private static final  String MUSIC_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/downloadedAudio.mp3";
    MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        final InteractivePlayerView ipv = (InteractivePlayerView) findViewById(R.id.ipv);
        ipv.setMax(100);
        ipv.setProgress(0);
        ipv.setOnActionClickedListener(this);

        final ImageView control = (ImageView) findViewById(R.id.control);
        control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!ipv.isPlaying()){
                    control.setBackgroundResource(R.drawable.pause);
                    mediaPlayer = new MediaPlayer();
                    try{
                        mediaPlayer.setDataSource(MUSIC_FILE_PATH);
                        mediaPlayer.prepare();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                    ipv.setMax(mediaPlayer.getDuration()/1000);
                    ipv.start();
                    mediaPlayer.start();
                }
                else{
                    ipv.stop();
                    ipv.setProgress(0);
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    control.setBackgroundResource(R.drawable.play);
                }
            }
        });
    }

    @Override
    public void onActionClicked(int i) {

    }
}

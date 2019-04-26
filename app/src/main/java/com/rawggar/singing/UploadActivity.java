package com.rawggar.singing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder;
import cafe.adriel.androidaudiorecorder.model.AudioChannel;
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate;
import cafe.adriel.androidaudiorecorder.model.AudioSource;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UploadActivity extends Activity {
    private final String TAG = "UploadActivity";

    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String AUDIO_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/recorded_audio.wav";

    private static boolean semaphore = true;
    Animation animRotate;

    OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        semaphore=true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);


        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        AndroidAudioRecorder.with(this)
                // Required
                .setFilePath(AUDIO_FILE_PATH)
                .setColor(ContextCompat.getColor(this, R.color.color_sign_gray))
                .setRequestCode(REQUEST_RECORD_AUDIO)

                // Optional
                .setSource(AudioSource.MIC)
                .setChannel(AudioChannel.STEREO)
                .setSampleRate(AudioSampleRate.HZ_48000)
                .setAutoStart(false)
                .setKeepDisplayOn(true)
                // Start recording
                .record();

        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {
//                Toast.makeText(getApplicationContext(),"Success",Toast.LENGTH_SHORT).show();
                // Great!
                Log.d(TAG, "onSuccess: Converted Audio");
            }
            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
//                Toast.makeText(getApplicationContext(),"Failure",Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onFailure: Could not convert Audio");
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
//                Toast.makeText(this, "Audio recorded successfully!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: Audio Recorded Successfully");
            } else if (resultCode == RESULT_CANCELED) {
//                Toast.makeText(this, "Audio was not recorded", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onActivityResult: Audio was not Recorded");
            }
        }
    }

    public void uploadAudio(View v){

        TextView processText = (TextView)findViewById(R.id.processingText);
        ImageButton photo_upload = (ImageButton)findViewById(R.id.btn_process);

        processText.setText("Processing...");

        animRotate = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate);
        photo_upload.startAnimation(animRotate);

        if(semaphore) {
            semaphore=false;
            cleanWAV();
        }
        else{}

    }

    public void processAudio(){

//        final ProgressDialog mProgressDialog = new ProgressDialog(this);
//        mProgressDialog.setIndeterminate(true);
//        mProgressDialog.setMessage("Processing...");
//        mProgressDialog.show();

        TextView processText = (TextView)findViewById(R.id.processingText);
        processText.setText("Processing...");

        // Use bounce interpolator with amplitude 0.2 and frequency 20

//        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
//        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.5, 30);
//        myAnim.setInterpolator(interpolator);
//        photo_upload.startAnimation(myAnim);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        //creating retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        //creating our api
        Api api = retrofit.create(Api.class);

        Call<processingModel> call = api.processAudio();
        call.enqueue(new Callback<processingModel>() {
            @Override
            public void onResponse(Call<processingModel> call, Response<processingModel> response) {

//                if(mProgressDialog.isShowing())
//                    mProgressDialog.dismiss();

                if(response.message()!=null){

//                    Toast.makeText(getApplicationContext(),response.message(),Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onResponse: "+ response.message());
                    downloadAudio();
                }
                else{
//                    Toast.makeText(getApplicationContext(),"null received",Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onResponse: Null Recieved");
                }
            }

            @Override
            public void onFailure(Call<processingModel> call, Throwable t) {
//                if(mProgressDialog.isShowing())
//                    mProgressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"Some Error Occurred in Processing",Toast.LENGTH_LONG).show();
            }
        });

    }

    public void downloadAudio(){
//        final ProgressDialog mProgressDialog = new ProgressDialog(this);
//        mProgressDialog.setIndeterminate(true);
//        mProgressDialog.setMessage("Downloading...");
//        mProgressDialog.show();
        TextView processText = (TextView)findViewById(R.id.processingText);
        processText.setText("Downloading...");
        // Use bounce interpolator with amplitude 0.2 and frequency 20

//        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
//        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.5, 30);
//        myAnim.setInterpolator(interpolator);
//        photo_upload.startAnimation(myAnim);

//        animRotate = AnimationUtils.loadAnimation(getApplicationContext(),
//                R.anim.rotate);
//        photo_upload.startAnimation(animRotate);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        //creating retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        //creating our api
        Api api = retrofit.create(Api.class);

        Call<ResponseBody> call = api.downloadAudio();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if(mProgressDialog.isShowing())
//                    mProgressDialog.dismiss();
                if(response.isSuccessful()){
                    boolean writtenToDisk = writeToDisk(response.body());
                    Log.d("Download",writtenToDisk+"");
//                    Toast.makeText(getApplicationContext(),"File downloaded successfully",Toast.LENGTH_LONG).show();
                    //convert
                    Log.d(TAG, "onResponse: File Downloaded Suussfully");
                    convertFile();
                }
                else{
                    Log.d("Download","Server Contact Failed");
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                if(mProgressDialog.isShowing())
//                    mProgressDialog.dismiss();
                Toast.makeText(getApplicationContext(),"Audio could not be Downloaded",Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void convertFile(){
        TextView processText = (TextView)findViewById(R.id.processingText);
        processText.setText("Converting.");
        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Converting...");
        mProgressDialog.show();
            File flacFile = new File(Environment.getExternalStorageDirectory(), "downloadedAudio.wav");
            IConvertCallback callback = new IConvertCallback() {
                @Override
                public void onSuccess(File convertedFile) {
                    // So fast? Love it!

                    if(mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onSuccess: Success Converting");

                    //now play the player
                    Intent i = new Intent(getApplicationContext(),PlayerActivity.class);
                    startActivity(i);
                }

                @Override
                public void onFailure(Exception error) {

                    if(mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    // Oops! Something went wrong
                }
            };
            AndroidAudioConverter.with(this)
                    // Your current audio file
                    .setFile(flacFile)

                    // Your desired audio format
                    .setFormat(AudioFormat.MP3)

                    // An callback to know when conversion is finished
                    .setCallback(callback)

                    // Start conversion
                    .convert();
    }

    public void cleanWAV(){
        TextView processText = (TextView)findViewById(R.id.processingText);
        processText.setText("Converting..");
        File flacFile = new File(Environment.getExternalStorageDirectory(), "recorded_audio.wav");
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!

//                Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onSuccess: Success Converting");


                cleanWAV2();
            }

            @Override
            public void onFailure(Exception error) {


            }
        };
        AndroidAudioConverter.with(this)
                // Your current audio file
                .setFile(flacFile)

                // Your desired audio format
                .setFormat(AudioFormat.MP3)

                // An callback to know when conversion is finished
                .setCallback(callback)

                // Start conversion
                .convert();
    }

    public void cleanWAV2(){
        final TextView processText = (TextView)findViewById(R.id.processingText);
        processText.setText("Converting...");
        File flacFile = new File(Environment.getExternalStorageDirectory(), "recorded_audio.mp3");
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!

//                Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onSuccess: Success Converting");

//                final ProgressDialog mProgressDialog = new ProgressDialog(getApplicationContext());
//                mProgressDialog.setIndeterminate(true);
//                mProgressDialog.setMessage("Uploading...");
//                mProgressDialog.show();
                //creating a file
                File file = new File(AUDIO_FILE_PATH);

                //creating request body for file
                RequestBody requestFile = RequestBody.create(MediaType.parse("audio/wav"), file);

                MultipartBody.Part body = MultipartBody.Part.createFormData("uploadedFile",file.getName(),requestFile);
                //The gson builder
                Log.d("MANISH",requestFile.contentType().toString());
                Gson gson = new GsonBuilder()
                        .setLenient()
                        .create();

                //creating retrofit object
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(Api.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();

                //creating our api
                Api api = retrofit.create(Api.class);

                //creating a call and calling the upload image method
                Call<MyResponse> call = api.uploadAudio(body);

                //finally performing the call

                processText.setText("Uploading...");
                call.enqueue(new Callback<MyResponse>() {
                    @Override
                    public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

//                        Toast.makeText(getApplicationContext(), "File Uploaded Successfully...", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "onResponse: File Uploaded Successfully");
                        processText.setText("Uploaded Successfully!");
                        //processAudio();
//                        if (mProgressDialog.isShowing())
//                            mProgressDialog.dismiss();

                        processAudio();
                    }

                    @Override
                    public void onFailure(Call<MyResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "onFailure: File not Uploaded Successfully");
//                        if (mProgressDialog.isShowing())
//                            mProgressDialog.dismiss();
                    }
                });

            }

            @Override
            public void onFailure(Exception error) {


            }
        };
        AndroidAudioConverter.with(this)
                // Your current audio file
                .setFile(flacFile)

                // Your desired audio format
                .setFormat(AudioFormat.WAV)

                // An callback to know when conversion is finished
                .setCallback(callback)

                // Start conversion
                .convert();
    }

    private boolean writeToDisk(ResponseBody body){
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(Environment.getExternalStorageDirectory().getPath() + "/downloadedAudio.wav");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("Download", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}

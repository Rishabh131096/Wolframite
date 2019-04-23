package com.rawggar.singing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private static final int REQUEST_RECORD_AUDIO = 0;
    private static final String AUDIO_FILE_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/recorded_audio.wav";

    OkHttpClient okHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                .setColor(ContextCompat.getColor(this, R.color.color_red))
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
                Toast.makeText(getApplicationContext(),"Success",Toast.LENGTH_SHORT).show();
                // Great!
            }
            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
                Toast.makeText(getApplicationContext(),"Failure",Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Audio recorded successfully!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Audio was not recorded", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void uploadAudio(View v){



        cleanWAV();



    }

    public void processAudio(){

        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Processing...");
        mProgressDialog.show();

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

                if(mProgressDialog.isShowing())
                    mProgressDialog.dismiss();

                if(response.message()!=null){

                    Toast.makeText(getApplicationContext(),response.message(),Toast.LENGTH_LONG).show();
                    downloadAudio();
                }
                else{
                    Toast.makeText(getApplicationContext(),"null received",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<processingModel> call, Throwable t) {
                if(mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                    Toast.makeText(getApplicationContext(),"failure on processing",Toast.LENGTH_LONG).show();
            }
        });

    }

    public void downloadAudio(){
        final ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.show();

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
                if(mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                if(response.isSuccessful()){
                    boolean writtenToDisk = writeToDisk(response.body());
                    Log.d("Download",writtenToDisk+"");
                    Toast.makeText(getApplicationContext(),"File downloaded successfully",Toast.LENGTH_LONG).show();
                    //convert
                    convertFile();
                }
                else{
                    Log.d("Download","Server Contact Failed");
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if(mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                Toast.makeText(getApplicationContext(),"File could not download",Toast.LENGTH_SHORT).show();

            }
        });
    }

    public void convertFile(){
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
                    Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();
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
        File flacFile = new File(Environment.getExternalStorageDirectory(), "recorded_audio.wav");
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!

                Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();

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

        File flacFile = new File(Environment.getExternalStorageDirectory(), "recorded_audio.mp3");
        IConvertCallback callback = new IConvertCallback() {
            @Override
            public void onSuccess(File convertedFile) {
                // So fast? Love it!

                Toast.makeText(getApplicationContext(), "Success Converting", Toast.LENGTH_SHORT).show();

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
                call.enqueue(new Callback<MyResponse>() {
                    @Override
                    public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                        Toast.makeText(getApplicationContext(), "File Uploaded Successfully...", Toast.LENGTH_LONG).show();
                        //processAudio();
//                        if (mProgressDialog.isShowing())
//                            mProgressDialog.dismiss();

                        processAudio();
                    }

                    @Override
                    public void onFailure(Call<MyResponse> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
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

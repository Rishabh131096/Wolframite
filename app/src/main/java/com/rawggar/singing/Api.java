package com.rawggar.singing;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Api {

        //the base URL for our API
        //make sure you are not using localhost
        //find the ip usinc ipconfig command
        String BASE_URL = "http://ec2-13-59-17-66.us-east-2.compute.amazonaws.com:3000";

        //this is our multipart request
        //we have two parameters on is name and other one is description
        @Multipart
        @POST("rec")
        Call<MyResponse> uploadAudio(@Part MultipartBody.Part file);

        @GET("process")
        Call<processingModel> processAudio();

        @GET("downloadAudio")
        Call<ResponseBody> downloadAudio();

}

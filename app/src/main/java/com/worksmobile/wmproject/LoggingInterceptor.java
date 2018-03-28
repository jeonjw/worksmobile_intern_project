package com.worksmobile.wmproject;

import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoggingInterceptor implements Interceptor {

    private String requestUrl;
    private DriveApi mDriveApi;
    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
//
//        System.out.println(chain.request().body());
//        System.out.println(chain.request().method());
//        System.out.println(chain.request().url());
        System.out.println(chain.proceed(original).isRedirect());
        System.out.println(chain.proceed(original).message());
        System.out.println(chain.proceed(original).body());
        System.out.println(chain.proceed(original).request().url());
        System.out.println(chain.proceed(original).networkResponse());



        return chain.proceed(original);

    }
}

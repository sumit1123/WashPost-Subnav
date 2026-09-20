package com.washingtonpost.android.comics.services;

import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.washingtonpost.android.comics.model.ComicStrip;
import com.washingtonpost.android.comics.model.ComicsDeserializer;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by muppallav
 */
public class ApiClient {

   /* private static final String COMICS_API_URL="https://comics-api-staging.wpdigital.net";
    private static final String COMICS_TOKEN= "f6abf562-41c9-4484-81f7-8b724331c657";*/

    private static long SIZE_OF_CACHE = 3 * 1024 * 1024;
    private final String HEADER_KEY_CACHE_CONTROL = "Cache-Control";
    private final String HEADER_VALUE_MAX_STALE = "public, only-if-cached, max-stale=";
    private final String COMICS_HTTP = "comics-http";
    private final String DATE_FORMAT = "yyyyMMdd";
    private final String baseUrl;
    private final File cacheDir;
    private final Context context;
    private final Point point;
    private boolean shouldOnlyReturnFreshResponse;

    public static boolean isConnectedOrConnecting(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    public ApiClient(String baseUrl, File cacheDir, Context context, Point point) {
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
        this.context = context;
        this.point = point;
    }

    public ApiClient(String baseUrl, File cacheDir, Context context, Point point, boolean shouldReturnOnlyNewResponse) {
        this(baseUrl, cacheDir, context, point);
        this.shouldOnlyReturnFreshResponse = shouldReturnOnlyNewResponse;
    }

    public Retrofit getClient() {

        Interceptor mCacheControlInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                if (chain == null) return null;
                Request request = chain.request();
                if (!isConnectedOrConnecting(context)) {
                    int maxStale = 60 * 60 * 24; // tolerate 1 day stale
                    request = request.newBuilder().header(HEADER_KEY_CACHE_CONTROL, HEADER_VALUE_MAX_STALE + maxStale).build();
                }
                Response originalResponse = chain.proceed(request);
                if (shouldOnlyReturnFreshResponse && (originalResponse != null)) {
                    /** cacheResponse is null for fresh network request's response
                     *  It is not null for 304(not modified) and when no connection is available
                    **/
                    if (originalResponse.cacheResponse() == null) {
                        return originalResponse;
                    } else {
                        MediaType contentType = MediaType.parse("application/json; charset=utf-8");
                        ResponseBody body = ResponseBody.create(contentType, "{}");
                        return originalResponse.newBuilder().body(body).build();
                    }
                }
                return originalResponse;
            }
        };

        // Create Cache
        Cache cache = null;
        try {
            cache = new Cache(new File(cacheDir, COMICS_HTTP), SIZE_OF_CACHE);
        } catch (Exception e) {
            //System.out.println("Unable to create the cache");
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new DefaultHeadersInterceptor());
        builder.interceptors().add(mCacheControlInterceptor);
        builder.cache(cache);
        OkHttpClient client = builder.build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getComicsGsonFactory()))
                .client(client)
                .build();
        return retrofit;
    }

    private Gson getComicsGsonFactory() {
        return new com.google.gson.GsonBuilder()
                .serializeNulls()
                .excludeFieldsWithModifiers()
                .setDateFormat(DATE_FORMAT)
                .registerTypeAdapter(ComicStrip.class, new ComicsDeserializer(point))
                .create();
    }
}

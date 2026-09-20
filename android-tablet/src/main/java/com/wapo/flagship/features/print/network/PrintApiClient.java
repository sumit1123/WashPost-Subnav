package com.wapo.flagship.features.print.network;

import android.content.Context;

import com.wapo.android.commons.util.Logger;


import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.wapo.flagship.model.PrintManifestResponse;
import com.wapo.flagship.util.ReachabilityUtil;
import com.washingtonpost.android.BuildConfig;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by Daniel Curaca Malito
 */
public class PrintApiClient {

    private static long SIZE_OF_CACHE = 3 * 1024 * 1024;
    private final String HEADER_KEY_CACHE_CONTROL = "Cache-Control";
    private final String HEADER_VALUE_MAX_STALE = "public, only-if-cached, max-stale=";
    private final String HEADER_VALUE_FORCE_UPDATE = "max-age=0";
    private final String PRINT_HTTP = "print-http";
    private final String baseUrl;
    private final File cacheDir;
    private final Context context;

    public PrintApiClient(String baseUrl, File cacheDir, Context context) {
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
        this.context = context;
    }

    public Retrofit getClient() {

        Interceptor mCacheControlInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {

                if (chain == null) return null;
                Request request = chain.request();
                if (!ReachabilityUtil.isConnected(context)) {
                    int maxStale = 60 * 60 * 24 * 14; // tolerate 14 days stale (in seconds)
                    request = request.newBuilder().header(HEADER_KEY_CACHE_CONTROL, HEADER_VALUE_MAX_STALE + maxStale).url(request.url().toString().replace("%2F", "/")).build();
                } else {
                    //This line causes the app to always download from the network if it is available.
                    request = request.newBuilder().header(HEADER_KEY_CACHE_CONTROL, HEADER_VALUE_FORCE_UPDATE).url(request.url().toString().replace("%2F", "/")).build();
                }
                Response originalResponse = chain.proceed(request);
                return originalResponse;
            }
        };

        // Create Cache
        Cache cache = null;
        try {
            cache = new Cache(new File(cacheDir, PRINT_HTTP), SIZE_OF_CACHE);
        } catch (Exception e) {
            Logger.e(PrintApiClient.class.getSimpleName(), "Unable to create Print cache");
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.interceptors().add(mCacheControlInterceptor);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            builder.addInterceptor(logging);
        }
        builder.cache(cache);
        OkHttpClient client = builder
                .addInterceptor(new DefaultHeadersInterceptor())
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit;
    }

    public interface PrintApiService {
        @GET("{printManifestPath}")
        Observable<PrintManifestResponse> getPrintManifestObs(@Path("printManifestPath") String printManifestPath);

    }

}

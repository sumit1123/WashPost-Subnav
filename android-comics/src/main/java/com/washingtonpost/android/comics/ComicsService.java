package com.washingtonpost.android.comics;

import android.content.Context;
import android.graphics.Point;
import android.os.Looper;
import android.os.Process;
import com.washingtonpost.android.comics.model.ComicStrip;
import com.washingtonpost.android.comics.services.ApiClient;
import com.washingtonpost.android.comics.services.ComicsApiService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class ComicsService {

    public static final String [] BUNDLE_CONFIG = {"android_1x", "android_2x", "android_3x","android_4x"};

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private static final short HOURS_BUFFER = -4;

    private final ComicsApiService comicsServiceApi;

    private final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(4, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r) {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    super.run();
                }
            };
        }
    }));

    public ComicsService(String baseUrl, Point screenSizeInLandscape, Context context) {
        ApiClient apiClient = new ApiClient(baseUrl, context.getCacheDir(), context, screenSizeInLandscape);
        this.comicsServiceApi = apiClient.getClient().create(ComicsApiService.class);
    }

    public ComicsService(String baseUrl, Point screenSizeInLandscape, boolean shouldOnlyReturnFreshResponse, Context context) {
        ApiClient apiClient = new ApiClient(baseUrl, context.getCacheDir(), context, screenSizeInLandscape, shouldOnlyReturnFreshResponse);
        this.comicsServiceApi = apiClient.getClient().create(ComicsApiService.class);
    }

    {
        TimeZone serverTimeZone = TimeZone.getTimeZone("EST");
        dateFormat.setTimeZone(serverTimeZone);
    }

    public Observable<Map<Date, ComicStrip>> getSpecificComic(final String comicID, final Date fromDate, final Date toDate) {
        String fromDateStr = dateFormat.format(fromDate);
        String toDateStr = dateFormat.format(toDate);

        Observable<Map<Date, ComicStrip>> obs = comicsServiceApi.getSpecificComicsObs(comicID, fromDateStr, toDateStr);
        if (isMainThread()) {
            obs = obs.subscribeOn(scheduler);
        }
        return obs;
    }

    public Observable<Map<Date, List<ComicStrip>>> getTodayComicsObservable() {
        final Calendar today = Calendar.getInstance();
        //Reduce hours
        today.add(Calendar.HOUR, HOURS_BUFFER);
        String todayDateString = dateFormat.format(today.getTime());
        Observable<Map<Date, List<ComicStrip>>> obs = comicsServiceApi.getComicsObservable(todayDateString, todayDateString);
        if (isMainThread()) {
            obs = obs.subscribeOn(scheduler);
        }
        return obs;
    }

    private static boolean isMainThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }
}

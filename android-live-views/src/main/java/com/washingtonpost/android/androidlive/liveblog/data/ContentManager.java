package com.washingtonpost.android.androidlive.liveblog.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.washingtonpost.android.androidlive.liveblog.model.LiveBlogFeed;
import com.washingtonpost.android.androidlive.liveblog.model.PrimeTimeUrl;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.androidlive.util.NetworkUtil;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by elamgodilj on 8/27/16.
 */

public class ContentManager {

    private final String TAG = "AndroidLive$ContentManager";
    private final PrimeTimeUrl PRIMETIME_URL;
    private final String LIVE_BLOG_PROXY_URL;
    private final int MAX_ENTRIES;
    private static final int REQUEST_FREQUENCY = 60;
    private static final String MAX_ENTRIES_KEY = "LIVE_BLOG_MAX_ENTRIES";
    private static final String SUBTYPES_KEY = "subtypes";
    private WeakReference<Context> contextWeakReference;


    public ContentManager(PrimeTimeUrl primetimeURL, String liveBlogProxyURL, int maxEntries, Context context) {
        this.PRIMETIME_URL = primetimeURL;
        this.LIVE_BLOG_PROXY_URL = liveBlogProxyURL;
        this.MAX_ENTRIES = maxEntries;
        this.contextWeakReference = new WeakReference<>(context);
        Logger.d(TAG, "ContentManager");
    }

    public String getRequestURL() {
        Logger.d(TAG, "getRequestURL");

        String requestUrl = LIVE_BLOG_PROXY_URL.replace(MAX_ENTRIES_KEY, "" + MAX_ENTRIES)
                + PRIMETIME_URL.getUrl();

        if (PRIMETIME_URL.getSubtypes().isEmpty()) {
            return requestUrl;
        }

        return Uri.parse(requestUrl)
                .buildUpon()
                .appendQueryParameter(SUBTYPES_KEY, TextUtils.join(",", PRIMETIME_URL.getSubtypes()))
                .build()
                .toString();
    }

    public LiveBlogFeed fetchData() {
        if (contextWeakReference == null) {
            return null;
        }
        Logger.d(TAG, "fetchData");
        String response = NetworkUtil.fetchData(getRequestURL(), contextWeakReference.get());
        return TextUtils.isEmpty(response) ? null : LiveBlogFeed.parseJson(response);
    }

    public Observable<List<LiveBlogFeed.LiveBlogFeedItem>> startFetchingDataPeriodically() {
        Logger.d(TAG, "startFetchingDataPeriodically");
        return Observable.interval(0, REQUEST_FREQUENCY, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Logger.e(TAG, "Error occurred", throwable);
                    }
                })
                .retry()
                .map(new Func1<Long, List<LiveBlogFeed.LiveBlogFeedItem>>() {
                    @Override
                    public List<LiveBlogFeed.LiveBlogFeedItem> call(Long aLong) {
                        LiveBlogFeed feed = fetchData();
                        if (feed == null) {
                            return null;
                        }

                        List<LiveBlogFeed.LiveBlogFeedItem> itemsList = feed.getFeed();
                        if (itemsList == null || itemsList.isEmpty()) {
                            return null;
                        }

                        Logger.d(TAG, "startFetchingDataPeriodically$$map1$Func1$call");
                        //Need only MAX_ENTRIES number of items
                        return itemsList.subList(0, Math.min(MAX_ENTRIES, itemsList.size()));
                    }
                });
    }
}

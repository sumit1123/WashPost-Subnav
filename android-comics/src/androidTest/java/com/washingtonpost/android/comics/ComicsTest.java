package com.washingtonpost.android.comics;

import android.app.Application;
import android.graphics.Point;
import android.test.ApplicationTestCase;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.util.AppContextUtils;
import com.washingtonpost.android.comics.model.ComicStrip;

import java.util.Date;
import java.util.List;
import java.util.Map;

import rx.Subscriber;

/**
 * Created by elamgodilj on 12/4/17.
 */
public class ComicsTest extends ApplicationTestCase {

    private final String TAG = "ComicsTest";

    public ComicsTest() {
        super(Application.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        AppContextUtils.INSTANCE.init(getContext(), "appName");
    }

    public void testSimpleComicsFetching() {
        final ComicsService comicsService = new ComicsService("https://comics-api-staging.wpdigital.net/api/v1/comics/",
                new Point(480, 640),
                getContext());

        comicsService.getTodayComicsObservable().subscribe(new Subscriber<Map<Date, List<ComicStrip>>>() {
            @Override
            public void onCompleted() {
                Logger.d(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Logger.e(TAG, "onError", e);
            }

            @Override
            public void onNext(Map<Date, List<ComicStrip>> dateListMap) {
                Logger.d(TAG, "onNext");
                Logger.d(TAG, "size is " + dateListMap.size());
            }
        });
    }


    public void testFreshResponseFetching() {
        // First do the regular fetch
        testSimpleComicsFetching();

        // Second time, request for fresh response
        final ComicsService comicsService = new ComicsService("https://comics-api-staging.wpdigital.net/api/v1/comics/",
                new Point(480, 640),
                true, getContext());

        comicsService.getTodayComicsObservable().subscribe(new Subscriber<Map<Date, List<ComicStrip>>>() {
            @Override
            public void onCompleted() {
                Logger.d(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Logger.e(TAG, "onError", e);
            }

            @Override
            public void onNext(Map<Date, List<ComicStrip>> dateListMap) {
                Logger.d(TAG, "onNext");
                Logger.d(TAG, "size is " + dateListMap.size());
                assertEquals(0, dateListMap.size());
            }
        });
    }
}
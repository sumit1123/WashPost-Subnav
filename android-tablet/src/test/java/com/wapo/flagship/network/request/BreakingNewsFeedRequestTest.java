//package com.wapo.flagship.network.request;
//
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import java.io.IOException;
//
//import rx.Observable;
//import rx.Subscriber;
//
///**
// * Created by elamgodilj on 4/8/16.
// *
// * If you want to run this Unit test just make a change in zsync-android's build.gradle file. Change the classpath
// * under dependencies of the gradle to use amazon's gradle instead of google.
// * Replace the google gradle with this one: com.amazon.device.tools.build:gradle:1.1.3.
// *
// * After hitting sync, you should now be able to select 'Unit Tests' under 'Test Artifact' under 'Build Variants'
// *
// *
// * Congrats. You should now be able to run Unit Tests.
// *
// *
// * Soon, this dependency on using amazon's gradle plugin will be removed.
// */
//public class BreakingNewsFeedRequestTest {
//
//    private final String BREAKING_NEWS_URL = "https://www.washingtonpost.com/pb/test-banner-breaking-news/?outputType=jsonFront";
//
//    @Test
//    public void testBreakingNewsFeedRequest() {
//
//        Observable.create(new Observable.OnSubscribe<Response>() {
//            OkHttpClient client = new OkHttpClient();
//
//            @Override
//            public void call(Subscriber<? super Response> subscriber) {
//                try {
//                    Response response = client.newCall(new Request.Builder().
//                            url(BREAKING_NEWS_URL).
//                            build()).
//                            execute();
//                    subscriber.onNext(response);
//                    subscriber.onCompleted();
//                    if (!response.isSuccessful()) subscriber.onError(new Exception("error"));
//                } catch (IOException e) {
//                    subscriber.onError(e);
//                }
//            }
//        }).subscribe(new Subscriber<Response>() {
//            @Override
//            public void onCompleted() {
//
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onNext(Response response) {
//                try {
//                    System.out.print("The response is " + response.body().string());
//                } catch (Exception e) {
//                    System.out.print("The exception is " + e.getMessage());
//                    Assert.fail(e.getMessage());
//                }
//            }
//        });
//
//    }
//}

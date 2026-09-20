//package com.wapo.flagship.network.request;
//
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;
//import com.wapo.flagship.json.LiveBlogFeed;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import java.io.IOException;
//
//import rx.Observable;
//import rx.Subscriber;
//
//import static org.junit.Assert.assertEquals;
//
///**
// * Created by elamgodilj on 4/7/16.
// *
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
//public class LiveBlogFeedRequestTest {
//
//    //Use for testing grid_id = "ba379bc7-e378-40bf-9f85-f4561a82d8a4";
//    private final String _url = "https://grid.wpdigital.net/gridservice/grid_id/gql?q=$and:%5B%7B%22addedTimestamp%22:%7B%22$gt%22:timestamp%7D%7D,%7B%22onGrid%22:true%7D,%7B%22className%22:%22com.washingtonpost.webapps.grid.entities.MethodeContent%22%7D%5D&limit=3";
//    private final String TIMESTAMP = "timestamp";
//    private final String TIMESTAMP_SAMPLE = "0";
//    final String GRID_ID = "grid_id";
//    private final String GRID_ID_SAMPLE = "ba379bc7-e378-40bf-9f85-f4561a82d8a4";
//
//    @Test
//    public void testLiveBlogFeedRequest() {
//
//        Observable.create(new Observable.OnSubscribe<Response>() {
//            OkHttpClient client = new OkHttpClient();
//
//            @Override
//            public void call(Subscriber<? super Response> subscriber) {
//                try {
//                    Response response = client.newCall(new Request.Builder().
//                            url(_url.replace(GRID_ID, GRID_ID_SAMPLE).replace(TIMESTAMP, TIMESTAMP_SAMPLE)).
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
//    }
//}

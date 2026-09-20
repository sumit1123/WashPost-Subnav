package volley;


import androidx.test.platform.app.InstrumentationRegistry;

import com.washingtonpost.android.volley.Cache;
import com.washingtonpost.android.volley.ExecutorDelivery;
import com.washingtonpost.android.volley.Network;
import com.washingtonpost.android.volley.NetworkResponse;
import com.washingtonpost.android.volley.Request;
import com.washingtonpost.android.volley.RequestQueue;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.ResponseDelivery;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.*;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestQueuePriorityTest {

    @Test
    public void testPriority() throws Exception {
        final CountDownLatch cdl = new CountDownLatch(4);
        final List<Integer> results = new ArrayList<>(4);
        Cache cache = new NoCache();
        Network network = new TestNetwork();
        final ResponseDelivery responseDelivery = new ExecutorDelivery(Executors.newSingleThreadExecutor());
        RequestQueue queue = new RequestQueue(cache, network, 1, responseDelivery);

        String url = "http://httpbin.org/get";
        Response.ErrorListener errorListener = new TestErrorListener();

        Listener<String> listener = new Listener<String>() {
            @Override
            public void onSuccess(String s, Request<String> request) {
                System.out.println("onSuccess " + request);
                results.add(((TestRequest) request).id);
                cdl.countDown();
            }
        };

        queue.add(new TestRequest(0, listener, errorListener, url + "?ignoreit").withPriority(Request.Priority.LOW));
        queue.add(new TestRequest(1, listener, errorListener, url + "?ignoreit2").withPriority(Request.Priority.LOW));
        queue.add(new TestRequest(2, listener, errorListener, url).withPriority(Request.Priority.LOW));
        queue.add(new TestRequest(3, listener, errorListener, url).withPriority(Request.Priority.HIGH));

        queue.start();

        if (!cdl.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("timed out");
        }

        //check the order of execution
        Assert.assertEquals(results.get(0).longValue(), 3);
        Assert.assertEquals(results.get(1).longValue(), 0);
        Assert.assertEquals(results.get(2).longValue(), 1);
        Assert.assertEquals(results.get(3).longValue(), 2);
    }

    @Test
    public void testResultDelivery() throws Exception {
        int requestCount = 5;
        final List<Integer> results = new ArrayList<>(requestCount);
        final CountDownLatch cdl = new CountDownLatch(requestCount);
        final File cacheDir = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "volley");
        Cache cache = new DiskBasedCache(cacheDir);
        Network network = new TestNetwork();
        final ResponseDelivery responseDelivery = new ExecutorDelivery(Executors.newSingleThreadExecutor());
        RequestQueue queue = new RequestQueue(cache, network, 1, responseDelivery);

        String url = "http://httpbin.org/get";
        Response.ErrorListener errorListener = new TestErrorListener();

        Listener<String> listener = new Listener<String>() {
            @Override
            public void onSuccess(String s, Request<String> request) {
                System.out.println("onSuccess " + request);
                results.add(((TestRequest) request).id);
                cdl.countDown();
            }
        };

        for (int i = 0; i < requestCount; i++) {
            Request.Priority priority = i == requestCount - 1 ? Request.Priority.HIGH : Request.Priority.LOW;
            queue.add(new TestRequest(i, listener, errorListener, url).withPriority(priority));
        }

        queue.start();

        if (!cdl.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("timed out");
        }

        Assert.assertEquals(results.get(0).longValue(), requestCount - 1);
    }

    interface Listener<T> {
        void onSuccess(T t, Request<T> request);
    }

    private static class TestErrorListener implements Response.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            throw new RuntimeException(error);
        }
    }

    private class TestRequest extends Request<String> {

        final int id;
        final Listener<String> success;

        public TestRequest(int id, Listener<String> success, Response.ErrorListener listener, String url) {
            super(Method.GET, url, listener);
            this.id = id;
            this.success = success;
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(String response) {
            success.onSuccess(response, this);
        }

        TestRequest withPriority(Priority priority) {
            setPriority(priority);
            return this;
        }

        @Override
        public String toString() {
            return "test; id = " + id + " priority: " + getPriority().ordinal();
        }
    }

    private static class TestNetwork implements Network {
        @Override
        public NetworkResponse performRequest(Request<?> request) throws VolleyError {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return new NetworkResponse(
                    new byte[0],
                    Collections.<String, String>emptyMap()
            );
        }
    }
}
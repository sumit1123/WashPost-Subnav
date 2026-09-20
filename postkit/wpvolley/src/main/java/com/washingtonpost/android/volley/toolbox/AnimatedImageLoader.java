package com.washingtonpost.android.volley.toolbox;

import android.graphics.Bitmap;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.volley.*;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Thad Cox
 */
public class AnimatedImageLoader extends ImageLoader {

    private static final boolean D = BuildConfig.DEBUG;
    private static final String DTAG = "[d][il]";
    /**
     * HashMap of Cache keys -> BatchedImageRequest used to track in-flight requests so
     * that we can coalesce multiple requests to the same URL into a single network request.
     */
    protected final HashMap<String, BatchedAnimatedImageRequest> mInFlightRequests = new HashMap<String, BatchedAnimatedImageRequest>();

    /** HashMap of the currently pending responses (waiting to be delivered). */
    private final HashMap<String, BatchedAnimatedImageRequest> mBatchedResponses = new HashMap<String, BatchedAnimatedImageRequest>();

    /** Handler to the main thread. */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** Runnable for in-flight response delivery. */
    private Runnable mRunnable;

    /** Amount of time to wait after first response arrives before delivering all responses. */
    private final static int mBatchResponseDelayMs = 100;

    /**
     * Constructs a new ImageLoader.
     *  @param queue      The RequestQueue to use for making image requests.
     * @param imageCache The cache to use as an L1 cache static images.
     * @param globalImageListener
     */
    public AnimatedImageLoader(RequestQueue queue, ImageCache imageCache, GlobalImageListener globalImageListener) {
        super(queue, imageCache, globalImageListener);
    }
    public AnimatedImageContainer get(String requestUrl, AnimatedImageListener imageListener, int maxWidth, int maxHeight) {
        return get(requestUrl, imageListener, maxWidth, maxHeight, Request.Priority.LOW, false);
    }

    public AnimatedImageContainer get(String requestUrl, AnimatedImageListener imageListener, int maxWidth, int maxHeight, Request.Priority priority) {
        return get(requestUrl, imageListener, maxWidth, maxHeight, priority, false);
    }

    public AnimatedImageContainer get(String requestUrl, AnimatedImageListener imageListener, int maxWidth, int maxHeight, Request.Priority priority, boolean isOnMainThreadOnly) {

        if (isOnMainThreadOnly) {
            // only fulfill requests that were initiated from the main thread.
            throwIfNotOnMainThread();
        }

        final String cacheKey = getCacheKey(requestUrl, maxWidth, maxHeight);

        //See if we have a cached bitmap for this
        Bitmap cachedBitmap = mCache != null ? mCache.getBitmap(cacheKey) : null;

        if(cachedBitmap != null){
            // Return the cached bitmap.
            AnimatedImageContainer container = new AnimatedImageContainer(cachedBitmap, requestUrl, null, null);
            imageListener.onResponse(container, true);
            return container;
        }

        // An Image doesn't exist in the cache (and we can't cache Movies yet) for this url, lets make a request and let the caller know to use the default
        AnimatedImageContainer imageContainer = new AnimatedImageContainer(null, requestUrl, cacheKey, imageListener);

        // Update the caller to let them know that they should use the default bitmap.
        if (isOnMainThreadOnly) {
            imageListener.onResponse(imageContainer, true);
        }

        BatchedAnimatedImageRequest request = mInFlightRequests.get(cacheKey);
        if (request == null) {
            request = mBatchedResponses.get(cacheKey);
        }
        if(request != null){
            // If it is, add this request to the list of listeners.
            updateRequestPriprity(priority, request);

            request.addContainer(imageContainer);
            return imageContainer;
        }

        // The request is not already in flight.  Send the new request to the network and track it
        Request<?> newRequest = new AnimatedImageRequest(requestUrl, new Response.Listener<Object>() {
            @Override
            public void onResponse(Object response) {
                onGetImageSuccess(cacheKey, response);
            }
        }, maxWidth, maxHeight, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onGetImageError(cacheKey, error);
            }
        });

        newRequest.setPriority(priority);

        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey, new BatchedAnimatedImageRequest(newRequest, imageContainer));
        return imageContainer;
    }

    private void updateRequestPriprity(Request.Priority priority, BatchedAnimatedImageRequest request) {
        if (request.mRequest.getPriority().ordinal() < priority.ordinal()){
            request.mRequest.setPriority(priority);
            mRequestQueue.updatePriority(request.mRequest);
        }
    }

    /**
     * Handler for when an image was successfully loaded.
     *
     * <br/><b>Note:</b> only Bitmaps are cached in an LRU cache.  Movies are not cached because we can't determine the size
     *
     * @param cacheKey The cache key that is associated with the image request.
     * @param response The bitmap or movie that was returned from the network.
     *
     */
    protected void onGetImageSuccess(String cacheKey, Object response) {
        if(response instanceof Bitmap){
            if(mCache != null){
                mCache.putBitmap(cacheKey, (Bitmap) response);
            }
        } else if(!(response instanceof Movie)){
            throw new IllegalArgumentException("onGetImageSuccess only takes a parameter of type Bitmap or Movie");
        }

        batchSuccess(cacheKey, response);
    }

    private void batchSuccess(String cacheKey, Object response) {
        BatchedAnimatedImageRequest request = mInFlightRequests.remove(cacheKey);

        if(request != null){
            //Update the response media
            request.mResponseMedia = response;

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }


    /**
     * Handler for when an image failed to load.
     *
     * @param cacheKey The cache key that is associated with the image request.
     * @param error
     */
    @Override
    protected void onGetImageError(String cacheKey, VolleyError error) {
        // Notify the requesters that something failed via a null result.
        // Remove this request from the list of in-flight requests.
        batchError(cacheKey, error);
    }

    private void batchError(String cacheKey, VolleyError error) {
        BatchedAnimatedImageRequest request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Set the error for this request
            request.setError(error);

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Starts the runnable for batched delivery of responses if it is not already started.
     * @param cacheKey The cacheKey of the response being delivered.
     * @param request The BatchedAnimatedImageRequest to be delivered.
     */
    private void batchResponse(String cacheKey, final BatchedAnimatedImageRequest request) {
        mBatchedResponses.put(cacheKey, request);

        // If we don't already have a batch delivery runnable in flight, make a new one.
        // Note that this will be used to deliver responses to all callers in mBatchedResponses.
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (BatchedAnimatedImageRequest bir : mBatchedResponses.values()) {
                        for (AnimatedImageContainer container : bir.mContainers) {
                            // If one of the callers in the batched request canceled the request
                            // after the response was received but before it was delivered,
                            // skip them.
                            if (container.mListener == null) {
                                continue;
                            }
                            if (bir.getError() == null) {
                                container.data = bir.mResponseMedia;
                                container.mListener.onResponse(container, false);
                                if (getGlobalImageListener() != null) {
                                    getGlobalImageListener().onResponse(container.getData(), container.getRequestUrl(), false);
                                }
                            } else {
                                container.mListener.onErrorResponse(bir.getError());
                                if (getGlobalImageListener() != null) {
                                    getGlobalImageListener().onErrorResponse(bir.mRequest.getUrl(), bir.getError());
                                }
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }

            };
            // Post the runnable.
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    public AnimatedImageContainer loadToCache(final String requestUrl, AnimatedImageListener listener, Request.Priority priority) {
        if (D) { Logger.d(DTAG, "Loading image into cache (priority "+priority+"): " + requestUrl); }
        final String cacheKey = getCacheKey(requestUrl, 0, 0);
        AnimatedImageContainer imageContainer = new AnimatedImageContainer(null, requestUrl, cacheKey, listener);
        BatchedAnimatedImageRequest request = mInFlightRequests.get(cacheKey);
        if(request != null){
            // If it is, add this request to the list of listeners.
            request.addContainer(imageContainer);
            updateRequestPriprity(priority, request);
            return imageContainer;
        }

        // The request is not already in flight.  Send the new request to the network and track it
        Request<?> newRequest = new CacheImageRequest(requestUrl, new Response.Listener<Object>() {
            @Override
            public void onResponse(Object response) {
                batchSuccess(cacheKey, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                batchError(cacheKey, error);
            }
        });
        newRequest.setPriority(priority);

        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey, new BatchedAnimatedImageRequest(newRequest, imageContainer));
        return imageContainer;
    }

    public interface AnimatedImageListener {
        public void onErrorResponse(VolleyError error);
        public void onResponse(final AnimatedImageContainer response, boolean isImmediate);
    }


    public class AnimatedImageContainer {
        private Object data;

        private final AnimatedImageListener mListener;

        /** The cache key that was associated with the request */
        private final String mCacheKey;

        /** The request URL that was specified */
        private final String mRequestUrl;

        public AnimatedImageContainer(Object data, String requestUrl, String cacheKey, AnimatedImageListener listener) {
            this.data = data;
            this.mCacheKey = cacheKey;
            this.mRequestUrl = requestUrl;
            this.mListener = listener;

        }

        public Object getData() {
            return data;
        }

        /**
         * Releases interest in the in-flight request (and cancels it if no one else is listening).
         */
        public void cancelRequest() {
            if (mListener == null) {
                return;
            }

            BatchedAnimatedImageRequest request = mInFlightRequests.get(mCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(this);
                if (canceled) {
                    mInFlightRequests.remove(mCacheKey);
                }
            } else {
                // check to see if it is already batched for delivery.
                request = mBatchedResponses.get(mCacheKey);
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this);
                    if (request.mContainers.size() == 0) {
                        mBatchedResponses.remove(mCacheKey);
                    }
                }
            }
        }

        public String getRequestUrl() {
            return mRequestUrl;
        }
    }



    /**
     * @author Thad Cox
     */
    public static interface MovieDataCache {
        public byte[] getMovie(String url);
        public void putMovie(String url, byte[] data);
    }

    /**
     * Wrapper class used to map a Request to the set of active ImageContainer objects that are
     * interested in its results.
     *
     * This is a copy of {@link com.washingtonpost.android.volley.toolbox.ImageLoader.BatchedImageRequest} with modifications.
     * See the original if any questions
     */
    protected class BatchedAnimatedImageRequest {
        /** The request being tracked */
        private final Request<?> mRequest;

        /** The result of the request being tracked by this item */
        private Object mResponseMedia;

        /** Error if one occurred for this response */
        private VolleyError mError;

        /** List of all of the active ImageContainers that are interested in the request */
        private final LinkedList<AnimatedImageContainer> mContainers = new LinkedList<AnimatedImageContainer>();

        /**
         * Constructs a new BatchedAnimatedImageRequest object
         * @param request The request being tracked
         * @param container The ImageContainer of the person who initiated the request.
         */
        public BatchedAnimatedImageRequest(Request<?> request, AnimatedImageContainer container) {
            mRequest = request;
            mContainers.add(container);
        }

        /**
         * Set the error for this response
         */
        public void setError(VolleyError error) {
            mError = error;
        }

        /**
         * Get the error for this response
         */
        public VolleyError getError() {
            return mError;
        }

        /**
         * Adds another ImageContainer to the list of those interested in the results of
         * the request.
         */
        public void addContainer(AnimatedImageContainer container) {
            mContainers.add(container);
        }

        /**
         * Detatches the bitmap container from the request and cancels the request if no one is
         * left listening.
         * @param container The container to remove from the list
         * @return True if the request was canceled, false otherwise.
         */
        public boolean removeContainerAndCancelIfNecessary(AnimatedImageContainer container) {
            mContainers.remove(container);
            if (mContainers.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

}

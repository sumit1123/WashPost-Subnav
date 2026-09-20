package com.wapo.flagship.network;

import android.graphics.Bitmap;

import com.washingtonpost.android.volley.Request;
import com.washingtonpost.android.volley.RequestQueue;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.toolbox.GlobalImageListener;
import com.wapo.flagship.network.request.ComicsImageRequest;

public class ComicsImageLoader extends ImageLoaderBase {
    /**
     * Constructs a new ImageLoader.
     *
     * @param queue      The RequestQueue to use for making image requests.
     * @param imageCache The cache to use as an L1 cache.
     */
    public ComicsImageLoader(RequestQueue queue, ImageCache imageCache, GlobalImageListener globalImageListener) {
        super(queue, imageCache,globalImageListener);
    }

    @Override
    protected Request<?> getRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight, Bitmap.Config decodeConfig, Response.ErrorListener errorListener) {
        return new ComicsImageRequest(url, listener, maxWidth, maxHeight, decodeConfig, errorListener);
    }
}

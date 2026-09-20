package com.wapo.flagship.network.request;

import android.graphics.Bitmap;
import com.washingtonpost.android.volley.NetworkResponse;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.ImageRequest;

import java.util.Calendar;

public class ComicsImageRequest extends ImageRequest {
    public ComicsImageRequest(String url, Response.Listener<Bitmap> listener, Response.ErrorListener errorListener) {
        this(url, listener, 0, 0, Bitmap.Config.RGB_565, errorListener);
    }

    public ComicsImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight, Bitmap.Config decodeConfig, Response.ErrorListener errorListener) {
        super(url, listener, maxWidth, maxHeight, decodeConfig, errorListener);
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        Response<Bitmap> result = super.parseNetworkResponse(response);
        if (result.cacheEntry != null && result.cacheEntry.softTtl == 0) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MONTH, 1);
            result.cacheEntry.softTtl = c.getTimeInMillis();
        }
        return result;
    }

    @Override
    public void deliverError(VolleyError error) {
        if (hasHadResponseDelivered()) {
            return;
        }
        super.deliverError(error);
    }
}

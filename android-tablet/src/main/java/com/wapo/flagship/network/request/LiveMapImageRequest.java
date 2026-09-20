package com.wapo.flagship.network.request;

import android.graphics.Bitmap;
import com.washingtonpost.android.volley.NetworkResponse;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.FreshCacheTtl;
import com.washingtonpost.android.volley.toolbox.ImageRequest;

/**
 * Created by elamgodilj on 2/19/16.
 */
@FreshCacheTtl
public class LiveMapImageRequest extends ImageRequest {

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url           URL of the image
     * @param listener      Listener to receive the decoded bitmap
     * @param maxWidth      Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight     Maximum height to decode this bitmap to, or zero for
     *                      none
     * @param decodeConfig  Format to decode the bitmap to
     * @param errorListener Error listener, or null to ignore errors
     */
    public LiveMapImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight, Response.ErrorListener errorListener) {
        super(url, listener, maxWidth, maxHeight, Bitmap.Config.RGB_565, errorListener);
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        //TODO REmove the instanceOf type in CacheDispatcher
        if (response == null) return null;
        Response<Bitmap> result = super.parseNetworkResponse(response);
        if (result.cacheEntry != null) {
            //Append the life of this cache in number of seconds
            result.cacheEntry.softTtl = System.currentTimeMillis() + 5000;
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

package com.wapo.flagship.network.request

import android.graphics.Bitmap
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.ImageRequest
import com.washingtonpost.android.volley.toolbox.ImageRequestMarker

/**
 * An image request that doesn't throw an error if a valid response was already delivered from cache
 */
@ImageRequestMarker
open class CacheFallbackImageRequest
    @JvmOverloads
    constructor(
        url: String?,
        listener: Response.Listener<Bitmap>?,
        maxWidth: Int = 0,
        maxHeight: Int = 0,
        decodeConfig: Bitmap.Config? = Bitmap.Config.RGB_565,
        errorListener: Response.ErrorListener?,
    ) : ImageRequest(
            url,
            listener,
            maxWidth,
            maxHeight,
            decodeConfig,
            errorListener,
        ) {
        /**
         * The time that will be used to add to the current time if softTtl is missing
         */
        open var softTtlExtension = 0L

        override fun parseNetworkResponse(response: NetworkResponse?): Response<Bitmap> {
            val result = super.parseNetworkResponse(response)
            if (softTtlExtension > 0 && result.cacheEntry != null && result.cacheEntry.softTtl == 0L) {
                result.cacheEntry.softTtl = softTtlExtension
            }
            return result
        }

        override fun deliverError(error: VolleyError?) {
            if (hasHadResponseDelivered()) {
                return
            }
            super.deliverError(error)
        }
    }

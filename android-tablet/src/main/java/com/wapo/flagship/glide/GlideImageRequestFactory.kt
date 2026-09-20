package com.wapo.flagship.glide

import android.graphics.BitmapFactory
import com.bumptech.glide.load.data.DataFetcher
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.volley.NetworkResponse
import com.washingtonpost.android.volley.Request
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.HttpHeaderParser
import com.washingtonpost.android.volley.toolbox.ImageRequestMarker
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class GlideImageRequestFactory {
    fun create(
        url: String,
        callback: DataFetcher.DataCallback<in InputStream>,
        priority: Request.Priority,
        headers: Map<String?, String?>?,
    ): Request<ByteArray> {
        if (headers?.get(NO_RESIZER) == "true") {
            return GlideImageRequestNoResize(url, callback, priority, headers.toMutableMap())
        }
        return GlideImageRequest(url, callback, priority, headers?.toMutableMap())
    }

    companion object {
        const val NO_RESIZER = "X-WAPO-NO-RESIZE"

        // Additional timeout to DefaultRetryPolicy's DEFAULT_TIMEOUT_MS for large images and gifs
        // to minimize connection & read timeouts.
        const val DEFAULT_TIMEOUT_MS = 25000
    }
}

class GlideImageRequestNoResize(
    url: String,
    callback: DataFetcher.DataCallback<in InputStream>,
    priority: Priority,
    headers: MutableMap<String?, String?>?,
) : GlideImageRequestBase(url, callback, priority, headers) {
    // no additional logic needed
}

@ImageRequestMarker
class GlideImageRequest(
    url: String,
    callback: DataFetcher.DataCallback<in InputStream>,
    priority: Priority,
    headers: MutableMap<String?, String?>?,
) : GlideImageRequestBase(url, callback, priority, headers) {
    // no additional logic needed
}

open class GlideImageRequestBase(
    url: String,
    val callback: DataFetcher.DataCallback<in InputStream>,
    private val mPriority: Priority,
    private val headers: MutableMap<String?, String?>?,
) : Request<ByteArray>(Method.GET, GlideImageRequestFactory.DEFAULT_TIMEOUT_MS, url, null) {
    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> {
        if (!isCanceled) {
            val fromCache = response.headers["Volley-Location"] == "disk"
            if (fromCache) {
                // an image cache entry returned from disk doesn't contain the image data
                // instead, response.data represents the path to the image file
                val filePath = String(response.data, Charsets.UTF_8)
                val file = File(filePath)
                callback.onDataReady(file.inputStream())
            } else {
                callback.onDataReady(ByteArrayInputStream(response.data))
            }
        }
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: ByteArray?) {
    }

    override fun parseNetworkError(volleyError: VolleyError?): VolleyError {
        var glideOnLoadFailedStatus: String? = null
        try {
            if (!isCanceled && volleyError != null) {
                callback.onLoadFailed(volleyError)
            }
        } catch (exception: Exception) {
            glideOnLoadFailedStatus = exception.stackTraceToString()
        }

        // Log non-network errors only
        val isNetworkError = !AppContextUtils.isConnectingOrConnected()

        if (!isNetworkError) {
            val builder =
                EventLog
                    .Builder()
                    .setMessage("Image Load Error")
                    .setModule(LogModules.GLIDE)
                    .set("desired_width", AppContextUtils.getDeviceWidthPixels())
                    .set("url", url)
                    .setErrorCode(volleyError?.networkResponse?.statusCode)
                    .setErrorMessage(volleyError?.message)
            if (!glideOnLoadFailedStatus.isNullOrEmpty()) {
                builder.set("glide_on_load_failed_status", glideOnLoadFailedStatus)
            }
            RemoteLog.e(AppContextUtils.appContext, builder.build())
        }
        return super.parseNetworkError(volleyError)
    }

    override fun getPriority(): Priority = mPriority

    override fun getHeaders(): MutableMap<String?, String?> = headers ?: mutableMapOf()
}

package com.wapo.flagship.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.washingtonpost.android.volley.Request
import com.washingtonpost.android.volley.RequestQueue
import java.io.InputStream

class VolleyStreamFetcher(
    val requestQueue: RequestQueue,
    val model: GlideUrl,
    val requestFactory: GlideImageRequestFactory,
) : DataFetcher<InputStream> {
    companion object {
        const val HEADER_SHOULD_BYPASS_CACHE = "shouldBypassCache"
    }

    @Volatile
    private var request: Request<ByteArray>? = null

    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in InputStream>,
    ) {
        request =
            requestFactory.create(
                model.toStringUrl(),
                callback,
                glideToVolleyPriority(priority),
                model.headers,
            )
        val shouldBypassCache = model.headers[HEADER_SHOULD_BYPASS_CACHE]?.toBoolean() ?: false
        request?.setShouldBypassCache(shouldBypassCache)
        requestQueue.add(request)
    }

    private fun glideToVolleyPriority(priority: Priority): Request.Priority =
        when (priority) {
            Priority.IMMEDIATE -> Request.Priority.IMMEDIATE
            Priority.HIGH -> Request.Priority.HIGH
            Priority.NORMAL -> Request.Priority.NORMAL
            Priority.LOW -> Request.Priority.LOW
        }

    override fun cleanup() {
    }

    override fun cancel() {
        request?.cancel()
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java

    override fun getDataSource(): DataSource = DataSource.REMOTE
}

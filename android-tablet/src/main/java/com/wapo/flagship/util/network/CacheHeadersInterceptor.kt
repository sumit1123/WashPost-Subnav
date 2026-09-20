package com.wapo.flagship.util.network

import com.wapo.flagship.base.Cacheable
import okhttp3.Interceptor
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reads the [Cacheable] tag object from the request and if the cache tag presents,
 * adds a proper "If-Modified-Since" header
 */
class CacheHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val cache = chain.request().tag(Cacheable::class.java)
        val updatedAt = cache?.lastUpdated() ?: 0L
        var request = chain.request()
        if (updatedAt > 0L) {
            val dateString = Date(updatedAt).toIfModifiedSinceFormat()
            request =
                chain
                    .request()
                    .newBuilder()
                    .addHeader(IF_MODIFIED_SINCE, dateString)
                    .build()
        }
        return chain.proceed(request)
    }

    companion object {
        const val IF_MODIFIED_SINCE = "If-Modified-Since"
    }

    private fun Date.toIfModifiedSinceFormat(): String {
        val dateFormat =
            SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                Locale.ENGLISH,
            )
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(this)
    }
}

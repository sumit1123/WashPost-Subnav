package com.wapo.flagship.util.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Interceptor that allows to set a custom timeout per request
 */
class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val header = request.header(WP_TIMEOUT)
        val timeOutMs = parseTimeOut(header).coerceAtLeast(15000)
        val builder: Request.Builder = request.newBuilder()
        builder.removeHeader(WP_TIMEOUT)
        return chain
            .withConnectTimeout(timeOutMs, TimeUnit.MILLISECONDS)
            .withReadTimeout(timeOutMs, TimeUnit.MILLISECONDS)
            .proceed(builder.build())
    }

    private fun parseTimeOut(header: String?): Int =
        if (header != null) {
            try {
                header.toInt()
            } catch (t: Throwable) {
                0
            }
        } else {
            0
        }

    companion object {
        /**
         * Custom header to set timeout per request
         */
        const val WP_TIMEOUT = "WP_TIMEOUT"
    }
}

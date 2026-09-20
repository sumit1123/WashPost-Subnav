/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.android.commons.retrofit

import com.wapo.android.commons.util.AppContextUtils
import okhttp3.Interceptor
import okhttp3.Response

/** TODO It would be good to move this class to [com.wapo.networkutils] some point.
 * Note: It will create a circular dependency between [com.wapo.networkutils] and [com.wapo.android.commons]
 * which will need to be resolved.
 *
 * This class will depend on [com.wapo.android.commons] to import [com.wapo.android.commons.util.AppContextUtils]
 * and [com.wapo.android.commons.config.ConfigProcessor] will depend on [com.wapo.networkutils] to import
 * this class (and possibly other dependency issues; that was just the first I saw when trying to move
 * this class) */

class DefaultHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder().apply {
            for (header in headers) {
                header(header.key, header.value)
            }
        }.build()
        return chain.proceed(request)
    }

    companion object {
        var headers = mutableMapOf<String, String>()
            private set

        init {
            headers.apply {
                this["CLIENT-APP"] = "android-classic"
                this["User-Agent"] = AppContextUtils.appApiUserAgent
            }
        }
    }
}
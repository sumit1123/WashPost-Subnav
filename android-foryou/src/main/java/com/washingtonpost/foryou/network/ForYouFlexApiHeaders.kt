package com.washingtonpost.foryou.network

import com.wapo.android.commons.util.AppContextUtils
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds necessary headers for ForYou Flex API
 */
class ForYouFlexApiHeaders : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain
            .request()
            .newBuilder()
            .header("Accept", "*/*")
            .build()
        return chain.proceed(request)
    }
}
package com.wapo.networkutils.interceptors

import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.paywall.PaywallService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class AuthenticationInterceptor(private val serviceName: String, private val exclusionPathList: List<String> = listOf()) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return if (PaywallService.getInstance()?.loggedInUser == null && exclusionPathList.none { chain.request().url.toString().contains(it) }) {
            val message = "Request blocked by interceptor: $serviceName requires the user to be logged in."
            Logger.d(TAG, message)
            Response.Builder()
                .code(401)
                .message(message)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .request(chain.request())
                .body("Request blocked".toResponseBody("text/plain".toMediaTypeOrNull()))
                .build()
        } else {
            chain.proceed(chain.request())
        }
    }

    companion object {
        private val TAG = AuthenticationInterceptor::class.java.name
    }
}
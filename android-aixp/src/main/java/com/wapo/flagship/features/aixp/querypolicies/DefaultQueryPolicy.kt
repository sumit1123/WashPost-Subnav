/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.querypolicies

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.aixp.base.AI_XP_REQUEST_TIMEOUT_MS
import com.wapo.flagship.features.aixp.base.Cacheable
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.network.APIResult
import java.net.HttpURLConnection

/**
 * This is a default query policy that will mostly be used with a few exceptions.
 */
open class DefaultQueryPolicy<T>(private val cacheFallback: Boolean = true) : QueryPolicy<T> {

    private var cache: T? = null
    private var timeOut = AI_XP_REQUEST_TIMEOUT_MS

    override fun needUpdate(t: T?): Boolean {
        cache = t
        if (t is Cacheable) {
            return t.getTimeToLive() < System.currentTimeMillis()
        }
        return t == null
    }

    override fun onResponse(result: APIResult<T>): Status<out T> {
        val cache = this.cache
        return when (result) {
            is APIResult.Success -> {
                if (result.data != null) {
                    Status.Network(result.data)
                } else {
                    if (cache != null && cacheFallback) {
                        Status.Cache(cache)
                    } else {
                        Status.Error("response body is empty")
                    }
                }
            }
            is APIResult.Failure -> {
                Logger.d("DefaultQueryPolicy", "Retrofit response failed, ${result.rawResponse}")
                when (result.statusCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        if (cache != null) {
                            Status.Cache(cache)
                        } else {
                            // should never happen
                            Status.Error("server returned 304 but cache is missing")
                        }
                    }
                    else -> {
                        Status.Error("${result.rawResponse}")
                    }
                }
            }
            is APIResult.NetworkError -> {
                if (cache != null && cacheFallback) {
                    Status.Cache(cache)
                } else {
                    Status.Error(result.error.localizedMessage.orEmpty())
                }
            }
        }
    }

    override fun timeOut(): Int {
        return timeOut
    }
}
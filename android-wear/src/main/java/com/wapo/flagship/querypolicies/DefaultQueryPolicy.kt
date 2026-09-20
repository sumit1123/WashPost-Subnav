/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.querypolicies

import com.wapo.android.commons.util.LogUtil
import com.squareup.moshi.Moshi
import com.wapo.flagship.base.Cacheable
import com.wapo.flagship.features.articles2.models.Article415
import com.wapo.flagship.json.NativeFourFifteen
import com.wapo.flagship.models.Status
import com.wapo.flagship.network.APIResult
import java.net.HttpURLConnection

/**
 * This is a default query policy that will mostly be used with a few exceptions.
 */
open class DefaultQueryPolicy<T>(private val cacheFallback: Boolean = true) : QueryPolicy<T> {

    private var cache: T? = null
    var timeOut = 2500

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
                LogUtil.d("Retrofit response failed ", result.rawResponse ?: "")
                when (result.statusCode) {
                    NativeFourFifteen.RESPONSE_CODE -> {
                        Status.Error415(parse415Response(result.rawResponse))
                    }
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

    private fun parse415Response(rawResponse: String?): Article415? {
        rawResponse ?: return null
        return try {
            return Moshi.Builder().build().adapter(Article415::class.java)
                .fromJson(rawResponse)
        } catch (e: Exception) {
            null
        }
    }

}
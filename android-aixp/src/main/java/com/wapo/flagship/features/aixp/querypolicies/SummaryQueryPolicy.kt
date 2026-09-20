/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.querypolicies

import com.wapo.android.commons.util.Logger
import com.squareup.moshi.Moshi
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackResponse
import com.wapo.flagship.features.aixp.models.FeedbackSubmissionDetail
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.network.APIResult
import java.net.HttpURLConnection

class SummaryQueryPolicy<T>(
    private val articleLmt: Long,
    private val cacheFallback: Boolean = true
) : DefaultQueryPolicy<T>(cacheFallback) {

    private var cache: T? = null

    override fun needUpdate(t: T?): Boolean {
        cache = t
        return true
    }

    override fun onResponse(result: APIResult<T>): Status<out T> {
        val cache = this.cache
        Logger.d("SummaryQueryPolicy", "Retrofit onResponse $result")
        return when (result) {
            is APIResult.Success -> {
                if (result.data != null) {
                    Status.Network(result.data)
                } else {
                    if (cache != null && cacheFallback) {
                        Status.Cache(cache)
                    } else {
                        Status.Error("Empty response")
                    }
                }
            }
            is APIResult.Failure -> {
                Logger.d("SummaryQueryPolicy", "Retrofit response failed ${result.rawResponse}")
                when (result.statusCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        val error = parseErrorResponse(result.rawResponse)
                        Status.Error("${error.toString()}\ncode: ${result.statusCode}")
                    }

                    else -> {
                        Status.Error("${result.rawResponse}\ncode: ${result.statusCode}")
                    }
                }
            }
            is APIResult.NetworkError -> {
                if (cache != null && cacheFallback) {
                    Status.Cache(cache)
                } else if (!AppContextUtils.isConnectingOrConnected()) {
                    Status.Error("You're offline. Please check your connection and try again.")
                } else {
                    Status.Error(result.error.localizedMessage.orEmpty())
                }
            }
        }
    }

    private fun parseErrorResponse(rawResponse: String?): ArticleSummaryFeedbackResponse? {
        rawResponse ?: return null
        return try {
            return Moshi.Builder().build().adapter(ArticleSummaryFeedbackResponse::class.java)
                .fromJson(rawResponse)
        } catch (e: Exception) {
            return ArticleSummaryFeedbackResponse(
                FeedbackSubmissionDetail("Unable to parse error response", "${e.message}")
            )
        }
    }
}
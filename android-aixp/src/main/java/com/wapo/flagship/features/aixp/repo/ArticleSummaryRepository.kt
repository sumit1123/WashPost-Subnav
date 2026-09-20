/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.URLParser
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.aixp.BuildConfig
import com.wapo.flagship.features.aixp.base.BaseRepo
import com.wapo.flagship.features.aixp.models.ArticleMeta
import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackRequest
import com.wapo.flagship.features.aixp.models.ArticleSummaryFeedbackResponse
import com.wapo.flagship.features.aixp.models.FeedbackSubmissionDetail
import com.wapo.flagship.features.aixp.models.ArticleRealtimeSummary
import com.wapo.flagship.features.aixp.models.Status
import com.wapo.flagship.features.aixp.models.ArticleSummary
import com.wapo.flagship.features.aixp.models.SummaryRemoteConfig
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.aixp.querypolicies.Query
import com.wapo.flagship.features.aixp.services.ArticleFeedbackService
import com.wapo.flagship.features.aixp.services.ArticleSummaryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class ArticleSummaryRepository(
    private val summaryService: ArticleSummaryService,
    private val feedbackService: ArticleFeedbackService,
    private val summaryRemoteConfig: SummaryRemoteConfig
) : BaseRepo<ArticleRealtimeSummary> {

    override fun fetchData(
        query: Query<ArticleRealtimeSummary>,
        viewModelScope: CoroutineScope?,
        coroutineContext: CoroutineContext?
    ): LiveData<Status<out ArticleRealtimeSummary>> {
        return MutableLiveData<Status<out ArticleRealtimeSummary>>()
    }

    fun fetchData(
        articleMeta: ArticleMeta,
        query: Query<ArticleRealtimeSummary>,
        viewModelScope: CoroutineScope?,
        coroutineContext: CoroutineContext?
    ): LiveData<Status<out ArticleRealtimeSummary>> {
        val liveData = MutableLiveData<Status<out ArticleRealtimeSummary>>()
        viewModelScope?.launch(Dispatchers.IO) {
            val status = fetchArticleSummary(articleMeta, query)
            liveData.postValue(status)
        }
        return liveData
    }

    private suspend fun fetchArticleSummary(
        meta: ArticleMeta,
        query: Query<ArticleRealtimeSummary>
    ): Status<out ArticleRealtimeSummary> {
        val endpoint = summaryRemoteConfig.realtimeSummaryUrl ?: return Status.Error("Config is empty")
        val urlPath = URLParser(query.url).getPath()
        val data = summaryService.getArticleSummary(
            endpoint,
            timeoutMs = if (BuildConfig.DEBUG) 15_000 else 2_500,
            contentId = meta.contentId,
            url = urlPath
        )
        Logger.d(
            "ArticleSummary",
            "Fetched from Network, urlPath=$urlPath, articleUrl=${query.url}"
        )
        val status = query.queryPolicy.onResponse(data)
        if (status is Status.Error) {
            EventLog.Builder().apply {
                setMessage("Summary Load Error")
                setModule(LogModules.ARTICLES)
                setContentUrl(query.url)
                setForceUpload()
                if (data is APIResult.Failure) {
                    setErrorCode(data.statusCode)
                    setErrorMessage(data.rawResponse)
                }
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
        }
        return status
    }

    suspend fun submitSummary(
        contentId: String?,
        summary: ArticleSummary,
        rating: Int,
        feedback: String?,
        fromRealtimeSummary: Boolean?
    ): Status<out ArticleSummaryFeedbackResponse> {
        val endpoint = if (fromRealtimeSummary == true)
            summaryRemoteConfig.realtimeFeedbackUrl
        else
            null
        endpoint ?: return Status.Error("Config is empty")
        return withContext(Dispatchers.IO) {
            val result = feedbackService.submitFeedback(
                endpoint,
                timeoutMs = 30000,
                request = ArticleSummaryFeedbackRequest(
                    contentId = contentId,
                    url = summary.url,
                    userId = AppContextUtils.getUniqueDeviceId(),
                    rating = rating,
                    feedback = feedback
                )
            )
            when (result) {
                is APIResult.Success -> {
                    if (result.data != null) {
                        Status.Network(result.data)
                    } else {
                        Status.Error("Submitted Successfully!")
                    }
                }
                is APIResult.Failure -> {
                    EventLog.Builder().apply {
                        setMessage("Summary Feedback Submission Error")
                        setModule(LogModules.ARTICLES)
                        setContentUrl(summary.url)
                        set("id", contentId)
                        set("rating", rating)
                        set("feedback", feedback)
                        setErrorMessage(result.rawResponse)
                        setErrorCode(result.statusCode)
                        setForceUpload()
                    }.run {
                        if (AppContextUtils.isConnectingOrConnected()) {
                            RemoteLog.e(AppContextUtils.appContext, build())
                        }
                    }
                    val error  = parseFeedbackResponse(result.rawResponse)
                    Status.Error("${error.toString()}\ncode: ${result.statusCode}")
                }
                is APIResult.NetworkError -> {
                    if (!AppContextUtils.isConnectingOrConnected()) {
                        Status.Error("You're offline. Please check your connection and try again.")
                    } else {
                        Status.Error(result.error.localizedMessage.orEmpty())
                    }
                }
            }

        }
    }

    private fun parseFeedbackResponse(rawResponse: String?): ArticleSummaryFeedbackResponse? {
        rawResponse ?: return null
        return try {
            return Moshi.Builder().build().adapter(ArticleSummaryFeedbackResponse::class.java)
                .fromJson(rawResponse)
        } catch (e: Exception) {
            return ArticleSummaryFeedbackResponse(FeedbackSubmissionDetail("Unable to parse response.", "${e.message}"))
        }
    }

    companion object {
        const val WP_TIMEOUT = "WP_TIMEOUT"
    }
}
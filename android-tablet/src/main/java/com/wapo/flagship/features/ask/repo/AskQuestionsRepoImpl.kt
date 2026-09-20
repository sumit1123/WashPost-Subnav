// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.ask.repo

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.aixp.BuildConfig
import com.wapo.flagship.features.aixp.network.APIResult
import com.wapo.flagship.features.ask.cache.AskQuestionsCache
import com.wapo.flagship.features.ask.models.AskQuestionsResponse
import com.wapo.flagship.features.ask.models.CategoryItem
import com.wapo.flagship.features.ask.models.QuestionItem
import com.wapo.flagship.features.ask.services.AskQuestionsService
import com.wapo.flagship.features.ask.services.AskQuestionsServiceRequest
import com.wapo.flagship.util.JUcidTracker
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import java.time.Instant
import javax.inject.Inject

class AskQuestionsRepoImpl
@Inject
constructor(
    private val service: AskQuestionsService,
    private val cache: AskQuestionsCache,
    private val remoteLogRepo: RemoteLogRepo
) : AskQuestionsRepo {
    private val timeout = if (BuildConfig.DEBUG) 2_000 else 2_000
    private val _defaultQuestions: MutableList<QuestionItem> = mutableListOf()
    override val defaultQuestions: List<QuestionItem> = _defaultQuestions

    private val _defaultCategories: MutableList<CategoryItem> = mutableListOf()
    override val defaultCategories: List<CategoryItem> = _defaultCategories

    override suspend fun getQuestions(skipCache: Boolean): APIResult<AskQuestionsResponse> {
        Logger.d(TAG, "getQuestions()")

        if (cache.isCacheValid() && !skipCache) {
            val cacheData = cache.getQuestionsList()
            if (cacheData != null) {
                Logger.d(TAG, "AskQuestions: cache was used")
                return APIResult.Success(cacheData)
            }
        }

        Logger.d(TAG, "AskQuestions: cache is not valid, using network")

        val apiResult = service.getFeedV2(
            timeout,  AskQuestionsServiceRequest(
                loginId = PaywallService.getInstance()?.loggedInUser?.uuid ?: "null",
                jucid = JUcidTracker.jUcid,
                deviceId = AppContextUtils.getUniqueDeviceId(),
                dateTime = Instant.now().toString(),
                experimentId = PaywallPrefHelper.getPrefTetroRctCookie() ?: "none"
            ))

        Logger.d(TAG, "AskQuestions: Result $apiResult")

        when (apiResult) {
            is APIResult.Success -> {
                val data = apiResult.data
                if (data != null) {
                    cache.saveQuestionsList(apiResult.data as AskQuestionsResponse)
                }
            }

            is APIResult.Failure -> {
                Logger.d(TAG, "AskQuestions: ERROR ${apiResult.rawResponse}")
                remoteLog("AskQuestions Failure", apiResult.rawResponse ?: "Failure")
                val cacheData =
                    cache.getQuestionsList() ?: AskQuestionsResponse(emptyList(), emptyList())
                return APIResult.Success(cacheData, true)
            }

            is APIResult.NetworkError -> {
                Logger.d(TAG, "AskQuestions: NETWORK ERROR: ${apiResult.error}")
                val cacheData =
                    cache.getQuestionsList() ?: AskQuestionsResponse(emptyList(), emptyList())
                return APIResult.Success(cacheData, true)
            }
        }

        return apiResult
    }

    private fun remoteLog(
        message: String,
        errorMessage: String,
    ) {
        EventLog
            .Builder()
            .apply {
                setMessage(message)
                setModule(LogModules.ASK_THE_POST)
                setErrorMessage(errorMessage)
            }.run {
                remoteLogRepo.e(build())
            }
    }

    override fun setDefaultQuestions(questions: List<QuestionItem>) {
        _defaultQuestions.apply {
            clear()
            addAll(questions)
        }
    }

    override fun setDefaultCategories(categories: List<CategoryItem>) {
        _defaultCategories.apply {
            clear()
            addAll(categories)
        }
    }

    companion object {
        const val TAG = "AskQuestionsRepository"
    }
}

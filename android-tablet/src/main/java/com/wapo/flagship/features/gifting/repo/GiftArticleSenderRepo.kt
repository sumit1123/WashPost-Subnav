package com.wapo.flagship.features.gifting.repo

import android.os.Build
import com.wapo.android.commons.constants.*
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.gifting.models.*
import com.wapo.flagship.features.gifting.services.GiftArticleService
import com.wapo.flagship.network.retrofit.network.APIResult
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.newdata.model.WpUser
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * A repository for gift article sender flow.
 * Two primary functions -
 * 1. Check number of remaining gift articles.
 * 2. Get the token and/or URL for the gift article.
 */
class GiftArticleSenderRepo
    @Inject
    constructor(
        private val giftArticleService: GiftArticleService,
        private val remoteLogRepo: RemoteLogRepo,
    ) {
        /**
         * Mediator live-date to post [RemainingCountApiStatus] to view model(s)
         */
        val remainingCountStatus = LiveEvent<RemainingCountApiStatus>()

        /**
         * Mediator live-date to post [RequestUrlApiStatus] to view model(s)
         */
        val requestUrlApiStatus = LiveEvent<RequestUrlApiStatus>()

        /**
         * Starts the n/w call to get the remaining count for gifting article with [articleUrl]
         */
        suspend fun getRemainingGiftCount(articleUrl: String) {
            val result =
                giftArticleService.getGiftArticleRemainingCount(
                    getHeaderMap(),
                    GiftArticleSenderRequestBody(articleUrl),
                )
            processRemainingCountResult(result)
        }

        /**
         * Starts the n/w call to get the token and/or URL for gifting article with [articleUrl]
         */
        suspend fun getGiftArticleTokenWithUrl(articleUrl: String) {
            val result =
                giftArticleService.getGiftArticleTokenWithUrl(
                    getHeaderMap(),
                    GiftArticleSenderRequestBody(articleUrl),
                )
            processGiftTokenResult(result)
        }

        /**
         * Adds required auth cookies to header
         */
        private fun getHeaderMap(): HashMap<String, String> {
            val headerMap = HashMap<String, String>()
            val loggedInUser = PaywallService.getInstance().loggedInUser
            headerMap[COOKIE] = getCookieString(loggedInUser)
            headerMap[CLIENT_APP] = PaywallService.getConnector().appName
            headerMap[CLIENT_APP_VERSION] = PaywallService.getConnector().appVersion
            headerMap[DEVICE_NAME] = Build.MANUFACTURER + "-" + Build.MODEL
            headerMap[OS_VERSION] = Build.VERSION.SDK_INT.toString()
            headerMap[DEVICE_ID] = PaywallService.getConnector().deviceId
            return headerMap
        }

        /**
         * Processes result of the [GiftArticleService.getGiftArticleRemainingCount] api call and converts the response into [RemainingCountApiStatus]
         */
        private fun processRemainingCountResult(result: APIResult<GiftArticleRemainingCountResponseBody>) {
            when (result) {
                is APIResult.Failure -> {
                    remainingCountStatus.postValue(
                        RemainingCountApiStatus.Failure,
                    )
                    if (AppContextUtils.isConnectingOrConnected()) {
                        val builder: EventLog.Builder = EventLog.Builder()
                        builder.setMessage("Gift Article Count Failed")
                            .setModule(LogModules.PAYWALL)
                            .setErrorMessage(result.getMessage())
                            .set("status", result.statusCode)
                        remoteLogRepo.e(builder.build())
                    }
                }
                is APIResult.NetworkError ->
                    remainingCountStatus.postValue(
                        RemainingCountApiStatus.Failure,
                    )
                is APIResult.Success -> {
                    val data = result.data
                    when {
                    /*
                        Success if -
                        Remaining count > 0
                        OR
                        Same article was already shared and Remaining count == 0
                     */
                        data?.status?.lowercase(Locale.US) == SUCCESS &&
                            data.remainingCount != null &&
                            (data.remainingCount > 0 || data.hasSharedArticle) ->
                            remainingCountStatus.postValue(
                                RemainingCountApiStatus.Success(
                                    data.remainingCount,
                                    data.hasSharedArticle,
                                ),
                            )
                    /*
                       Failure if -
                       status is FAILURE and state is NO_ARTICLES_LEFT_STATE (2256)
                       OR
                       status is SUCCESS and remaining count == 0 (Case with status is SUCCESS and remaining count == 0 for already shared article is handled above ^ )
                     */
                        (data?.status?.lowercase(Locale.US) == FAILURE && data.state == NO_ARTICLES_LEFT_STATE) ||
                            (data?.status?.lowercase(Locale.US) == SUCCESS && data.remainingCount != null && data.remainingCount == 0)
                        ->
                            remainingCountStatus.postValue(
                                RemainingCountApiStatus.NoRemainingArticles,
                            )
                        else -> remainingCountStatus.postValue(RemainingCountApiStatus.Failure)
                    }
                }
            }
        }

        /**
         * Processes result of the [GiftArticleService.getGiftArticleTokenWithUrl] api call and converts the response into [RequestUrlApiStatus]
         */
        private fun processGiftTokenResult(result: APIResult<GiftArticleTokenResponseBody>) {
            when (result) {
                is APIResult.Failure -> {
                    requestUrlApiStatus.postValue(
                        RequestUrlApiStatus.Failure,
                    )
                    if (AppContextUtils.isConnectingOrConnected()) {
                        val builder: EventLog.Builder = EventLog.Builder()
                        builder.setMessage("Gift Article Token Failed")
                            .setModule(LogModules.PAYWALL)
                            .setErrorMessage(result.getMessage())
                            .set("status", result.statusCode)
                        remoteLogRepo.e(builder.build())
                    }
                }
                is APIResult.NetworkError ->
                    requestUrlApiStatus.postValue(
                        RequestUrlApiStatus.Failure,
                    )
                is APIResult.Success -> {
                    val data = result.data
                    when {
                        data?.status?.lowercase(Locale.US) == SUCCESS && !data.url.isNullOrBlank() ->
                            requestUrlApiStatus.postValue(
                                RequestUrlApiStatus.Success(data.url),
                            )
                        else -> requestUrlApiStatus.postValue(RequestUrlApiStatus.Failure)
                    }
                }
            }
        }

        companion object {
            const val SUCCESS = "success"
            const val FAILURE = "failure"
            const val NO_ARTICLES_LEFT_STATE = 2256

            fun getCookieString(loggedInUser: WpUser) =
                "wapo_login_id=${loggedInUser.uuid}; wapo_secure_login_id=${loggedInUser.secureLoginID}"
        }
    }

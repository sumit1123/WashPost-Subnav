package com.washingtonpost.android.paywall.features.tetro

import android.os.Build
import com.wapo.android.commons.util.Logger
import android.webkit.URLUtil
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.CLIENT_ID
import com.wapo.android.commons.constants.CLIENT_IP
import com.wapo.android.commons.constants.CLIENT_USER_AGENT
import com.wapo.android.commons.constants.COOKIE
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.REQUEST_ID
import com.wapo.android.commons.logs.EventLog
import com.washingtonpost.android.paywall.BuildConfig
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.events.WallState
import com.washingtonpost.android.paywall.features.tetro.local.TetroLocalService
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleBody
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleObj
import com.washingtonpost.android.paywall.features.tetro.remote.TetroApiService
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper
import com.washingtonpost.android.paywall.helper.PaywallDbHelper
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.paywall.metering.MeteringPrefs
import com.washingtonpost.android.paywall.network.retrofit.APIResult
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

/**
 * This class handles syncing (On App Pause/Resume and Periodically) to the Tetro API
 * - Data sent : cookie acquired on first request, body of read articles
 * - Data received : meter count, meter limit, and list of weighted articles
 */
open class TetroManager(
    private var tetroRemoteService: TetroApiService.TetroApi,
    private var tetroLocalService: TetroLocalService
) {

    private val tetroDataMutex: Mutex = Mutex()
    var syncState: SyncState = SyncState.NotStarted
    var syncJob: Job? = null
    private var counter = 0
    private val coroutineScope = ProcessLifecycleOwner.get().lifecycleScope
    private var weightedArticles: HashMap<String, Float> = hashMapOf()
    private var articlesNotSynced = mutableListOf<ArticleStub>()

    /**
     * Call Tetro API to determine metering rules and walls to show.
     */
    suspend fun getWallResponse(
        currentArticle: ArticleObj,
        abTestVariants: String?,
        giftToken: String? = null
    ): WallState {
        // Cancel any active tetro sync that may have been called onPause or onResume of app
        if (syncJob?.isActive == true) {
            syncJob?.cancel()
        }

        // If gift token exists, process the gift token
        giftToken?.let {
            val giftState = processGiftToken(giftToken, currentArticle.url, abTestVariants)
            return WallState.GiftWall(giftState)
        }

        // Handle other wall cases such as Paywall and Regwall
        var response: APIResult<TetroResponse>? = null
        try {
            syncState = SyncState.InProgress
            response = getMeterData(
                getPreferencesRequestHeaders(),
                getArticlesPayloadBody(currentArticle),
                abTestVariants
            )
            when (response) {
                is APIResult.Success -> {
                    // Cache Tetro data to be used for offline mode
                    tetroLocalService.updateMeterData(response.data)

                    // Store cookies to maintain state and subAcctMgmt to keep track of free articles
                    tetroLocalService.storeCookies(response.headers)

                    return processTetroActions(
                        response.data?.action,
                        response.data?.data?.actionCodes,
                        response.data?.data?.prompts
                    )
                }

                is APIResult.NetworkError -> {
                    response.error.message?.apply {
                        Logger.d(Tag, this)
                    }
                    return WallState.Failed("${response.javaClass::getSimpleName} : ${response.error.message}")
                }

                is APIResult.Failure -> {
                    Logger.d(
                        Tag,
                        "Status Code ${response.statusCode} | Message: ${response.rawResponse}"
                    )
                    return WallState.Failed("${response.javaClass::getSimpleName} : Status Code ${response.statusCode} | Message: ${response.rawResponse}")
                }
            }
        } catch (e: Exception) {
            response = APIResult.Failure(500, e.message)
            return WallState.Failed("$Tag : ${e.message}")
        } finally {
            syncState = SyncState.Complete
            syncJob = null
            if (response !is APIResult.Success) {
                val message = when (response) {
                    is APIResult.Failure -> response.rawResponse
                    is APIResult.NetworkError -> response.error.message
                    else -> ""
                }
                PaywallService.getConnector().logE(EventLog.Builder().setMessage(message))
            }
        }
    }

    /**
     * Process Tetro Response actions
     * - 0 is no blocker
     * - 3 is paywall
     * - 6 is softwall (Not used in Apps yet)
     * - 9 is regwall
     */
    fun processTetroActions(
        action: Int?,
        actionCodes: List<String>?,
        prompts: List<Prompt>? = null
    ) =
        action?.let {
            Logger.d(Tag, "Tetro Response - Action: $action | Action Codes: $actionCodes | Prompts: $prompts")
            when {
                it == 0 -> {
                    if (prompts != null) {
                        WallState.MapWall(prompts)
                    } else {
                        WallState.NoWall
                    }
                }

                it == 3 -> WallState.Paywall(actionCodes ?: listOf())
                it == 6 -> {
                    if (prompts != null) {
                        WallState.MapWall(prompts)
                    } else {
                        WallState.Softwall(actionCodes ?: listOf())
                    }
                }

                it == 9 -> WallState.Regwall(actionCodes ?: listOf())
                else -> {
                    if (prompts != null) {
                        WallState.MapWall(prompts)
                    } else {
                        WallState.NoWall
                    }
                }
            }
        } ?: WallState.Failed("NullTetroResponse")

    /**
     * Sync with Tetro API.
     *  - [retryCount] will determine how many retries of call fails
     *  - Sync will happen in following conditions -> Not Subscribed | Has Connectivity | Sync not in Progress
     *  - Once Sync is complete, [updateStore] is called
     */
    fun sync(retryCount: Int = 0, abTestVariants: String?) {
        if (PaywallService.getInstance().isPremiumUser || !PaywallService.getConnector().isOnline || !PaywallService.getInstance().meteringServiceInstance.isTetroTurnedOn) {
            return
        }
        when (syncState) {
            SyncState.InProgress -> {
                Logger.e(Tag, "Sync already in Progress")
            }

            SyncState.NotStarted,
            SyncState.Complete -> {
                syncJob = coroutineScope.launch(Dispatchers.IO) {
                    if (PaywallService.getConnector().isOnline && tetroDataMutex.tryLock()) {
                        var response: APIResult<TetroResponse>? = null
                        try {
                            syncState = SyncState.InProgress
                            response = getMeterData(
                                getPreferencesRequestHeaders(),
                                getArticlesPayloadBody(),
                                abTestVariants
                            )
                            // Check if this job has been canceled
                            if (!isActive) {
                                return@launch
                            }
                            when (response) {
                                is APIResult.Success -> {
                                    // Cache Tetro data to be used for offline mode
                                    tetroLocalService.updateMeterData(response.data)

                                    // Store cookies to maintain state and subAcctMgmt to keep track of free articles
                                    tetroLocalService.storeCookies(response.headers)
                                }

                                is APIResult.NetworkError -> {
                                    response.error.message?.apply {
                                        Logger.d(Tag, this)
                                    }
                                }

                                is APIResult.Failure -> {
                                    Logger.d(
                                        Tag,
                                        "Status Code ${response.statusCode} | Message: ${response.rawResponse}"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            response = APIResult.Failure(500, e.message)
                        } finally {
                            tetroDataMutex.unlock()
                            syncState = SyncState.Complete
                            syncJob = null
                            if (response !is APIResult.Success && retryCount > 0) {
                                val retryUpdatedCount = retryCount - 1
                                sync(retryUpdatedCount, abTestVariants)
                            } else if (response !is APIResult.Success) {
                                val message = when (response) {
                                    is APIResult.Failure -> response.rawResponse
                                    is APIResult.NetworkError -> response.error.message
                                    else -> ""
                                }
                                PaywallService.getConnector()
                                    .logE(EventLog.Builder().setMessage(message))
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getMeterData(
        headers: HashMap<String, String>,
        body: ArticleBody,
        abVariants: String?
    ): APIResult<TetroResponse> {
        return if (PaywallConstants.USE_METERING_PROXY) {
            tetroRemoteService.getMeterProxyData(headers, body, abVariants)
        } else {
            tetroRemoteService.getMeterData(headers, body)
        }
    }


    /**
     * Call Tetro API and process gift token response.
     */
    suspend fun processGiftToken(token: String, url: String, abTestVariants: String?): GiftState {
        return if (PaywallService.getConnector().isOnline) {
            try {
                // Add gift article in POST request body
                val body = ArticleBody(listOf(ArticleObj(url, null)))
                val headers = getPreferencesRequestHeaders()
                // Add gift token to POST request header
                headers[GIFT_TOKEN_KEY] = token
                val response = getMeterData(
                    headers,
                    body,
                    abTestVariants
                )
                when (response) {
                    is APIResult.Success -> {
                        GiftState.getGiftStateFromActionCode(response.data?.data?.actionCodes)
                    }

                    is APIResult.NetworkError -> {
                        response.error.message?.apply {
                            PaywallService.getConnector().logE(EventLog.Builder().setMessage(this))
                        }
                        GiftState.Failure(response.error.message)
                    }

                    is APIResult.Failure -> {
                        val message =
                            "Gift Failure: Status Code ${response.statusCode} | Message: ${response.rawResponse}"
                        EventLog.Builder().apply {
                            setMessage("Gift Failure")
                            setErrorMessage(response.rawResponse)
                            setErrorCode(response.statusCode)
                        }.run {
                            PaywallService.getConnector().logE(this)
                        }
                        GiftState.Failure(message)
                    }
                }
            } catch (e: Exception) {
                GiftState.Failure("Gift Failure: ${e.message}")
            }
        } else {
            GiftState.Failure("Gift Failure: No Network")
        }
    }

    /**
     * Add/Update article to DB. Will be used for Tetro call payload. Following are the rules
     * when article is added/updated :
     *   1. Article is Metered Free
     *   2. Article is Free Content Type
     *   3. User has free trial articles (currently from Reddit promo only)
     */
    fun updateReadArticleList(articleStub: ArticleStub?) {
        if (articleStub == null) {
            return
        } else {
            articleStub.url?.apply {
                val isValid = URLUtil.isValidUrl(this)
                if (!isValid) {
                    return
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val existingArticle = if (articleStub.url != null) {
                    PaywallCounterHelper.getArticleByUrl(
                        articleStub.url,
                        PaywallDbHelper.PW_ARTICLE_TABLE
                    )
                } else {
                    null
                }

                if (existingArticle != null) {
                    PaywallCounterHelper.updateArticle(
                        articleStub,
                        PaywallDbHelper.PW_ARTICLE_TABLE
                    )
                } else {
                    PaywallCounterHelper.insertArticle(
                        articleStub,
                        PaywallDbHelper.PW_ARTICLE_TABLE
                    )
                }
            } catch (e: Exception) {
                PaywallService.getConnector().logHandledException(e)
            }
        }
    }

    /**
     * For Tetro POST request, get Json list of articles of [ArticleObj]
     *  - If 'articleList' is null or empty, default to using 'defaultBody'
     */
    private fun getArticlesPayloadBody(currentArticle: ArticleObj? = null): ArticleBody {
        articlesNotSynced =
            PaywallCounterHelper.getArticleListNotSynced(PaywallDbHelper.PW_ARTICLE_TABLE)
        var articleBody = defaultBody
        val articleObjList = mutableListOf<ArticleObj>()

        // Add all buffered read articles into list to be sent to Tetro
        if (!articlesNotSynced.isNullOrEmpty()) {
            articlesNotSynced.forEach {
                it.url?.let { url ->
                    articleObjList.add(
                        ArticleObj(
                            url,
                            it.timeStamp
                        )
                    )
                }
            }
        }

        // Add current article into list to be sent to Tetro
        currentArticle?.let {
            articleObjList.add(it)
        }

        // If list is not empty, update article body to include list
        if (articleObjList.isNotEmpty()) {
            articleBody = ArticleBody(articleObjList)
        }

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val body = gson.toJson(articleBody)
        Logger.d(Tag, "Body - \n $body")
        return articleBody
    }

    /**
     * For Tetro POST request, get list of headers.
     *  - 'cookie' header - All cookies that need to be passed to Tetro should be concatenated under this header.
     */
    private fun getPreferencesRequestHeaders(): HashMap<String, String> {
        val headerMap = HashMap<String, String>()
        headerMap[COOKIE] = getRequestCookies()
        headerMap[CLIENT_ID] = PaywallService.getConnector().clientId
        headerMap[CLIENT_IP] = PaywallService.getConnector().ipAddress
        headerMap[CLIENT_APP] = PaywallService.getConnector().appName
        headerMap[REQUEST_ID] = UUID.randomUUID().toString()
        headerMap[DEVICE_ID] = PaywallService.getConnector().deviceId
        headerMap[CLIENT_USER_AGENT] = PaywallService.getConnector().userAgent
        headerMap[CLIENT_APP_VERSION] = PaywallService.getConnector().appVersion
        headerMap[OS_VERSION] = Build.VERSION.SDK_INT.toString()
        headerMap[DEVICE_NAME] = Build.MANUFACTURER + "-" + Build.MODEL
        return headerMap
    }

    /**
     *  Tetro State Cookie - On the first request this is null but will be captured in the response header `Set-Cookie`
     *      holding the cookie name `wp_pwapi_ar`. On subsequent requests, this will hold
     *      article hash ids and timestamps for read articles. This header is the source of truth for metering
     *      state of user. Without this cookie, meter count will always be reset on sync.
     *  Cross-Device/Cross-Platform sync - We must respect promos offering free articles from other platforms.
     *      This requires passing account info to Tetro in the form of cookies.
     *      For logged in users, pass loginId, secureLoginId, and SubAcctMgmt cookie.
     *      Currently used only for Reddit promo.
     */
    private fun getRequestCookies(): String {
        val cookieString: StringBuilder = StringBuilder()

        // Bypasses Paywall for WaPo networks. Free articles will not be consumed on office wifi or VPN without this
        val bypassPaywall = PaywallPrefHelper.getPrefBypassPaywall()
        if (BuildConfig.DEBUG && bypassPaywall) {
            cookieString.appendCookie("debug_pwapi_ipDomainOn=1")
            cookieString.appendCookie("wp_tetro_preview=1")
        }

        cookieString.appendCookie(tetroLocalService.getStateCookie().orEmpty())

        if (PaywallService.getInstance().isWpUserLoggedIn) {
            cookieString.appendCookie("wapo_login_id=${PaywallService.getInstance().loggedInUser.uuid}")
            cookieString.appendCookie("wapo_secure_login_id=${PaywallService.getInstance().loggedInUser.secureLoginID}")
            cookieString.appendCookie("wapo_actmgmt=${PaywallService.getConnector().subAcctMgmt}") // tetro expects a different name than what we receive from Profile
        }

        // For some initiatives, geo cookie is required. For this reason we send it here.
        cookieString.appendCookie(PaywallPrefHelper.getPrefGeoCookie())

        cookieString.appendCookie(PaywallPrefHelper.getPrefTetroRctCookie())

        Logger.d(Tag, "Tetro Request Cookies: $cookieString")
        return cookieString.toString()
    }

    /**
     * Append cookie to existing cookie string.
     * If cookie string is not empty, append a semicolon before adding new cookie.
     */
    private fun StringBuilder.appendCookie(newCookie: String?) {
        if (newCookie.isNullOrEmpty()) {
            return
        }
        val updatedCookieString = this
        if (updatedCookieString.isNotEmpty()) {
            updatedCookieString.append("; ")
        }
        updatedCookieString.append(newCookie)
    }

    /**
     * Get map of weighted articles from Shared Preferences.
     */
    fun getWeightedArticles(skipTTLCheck: Boolean = false): HashMap<String, Float> {
        if (!skipTTLCheck && areArticleWeightsExpired()) {
            return hashMapOf()
        }
        if (weightedArticles.isNullOrEmpty()) {
            weightedArticles = try {
                MeteringPrefs.getArticleWeights(PaywallConstants.PW_ARTICLE_WEIGHTS)
            } catch (e: JsonSyntaxException) {
                PaywallService.getConnector().logHandledException(e)
                hashMapOf()
            }
        }
        return weightedArticles
    }

    private fun areArticleWeightsExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastUpdate = MeteringPrefs.getArticleWeightsLastFetched()
        val articleWeightsTtl =
            PaywallService.getInstance().meteringServiceInstance.tetroWeightedArticleTTL
        return currentTime - lastUpdate > articleWeightsTtl
    }

    /**
     * Update Meter Limit and Weight Articles map [updateWeightedArticles] in shared preferences.
     */
    private fun updateStore(response: TetroResponse?) {
        response?.apply {
            val meterCount = minOf(this.data.meterCount, this.data.meterLimit).toFloat()
            MeteringPrefs.setCurrentArticleCount(
                PaywallConstants.PW_CURRENT_ARTICLE_COUNT,
                meterCount
            )
            MeteringPrefs.setMeterCycleDays(data.meterCycleDays)
            MeteringPrefs.setMaxArticleLimit(this.data.meterLimit)
            MeteringPrefs.setFreeArticlesRemaining(this.data.freeTrialConsumptionCount)
            updateWeightedArticles(this.data.weightedArticles)
            PaywallService.getConnector()
                .trackTetroEvent(this.data.meterCount.toFloat(), this.data.meterState)
        }
    }

    /**
     * Take first 10 weighted articles map from Tetro POST response and transform it to a map with
     * article url as KEY and weight int as VALUE
     */
    private fun updateWeightedArticles(weightedArticlesResponse: HashMap<String, Map<String, String>>) {
        weightedArticles.clear()
        if (!weightedArticlesResponse.isNullOrEmpty()) {
            weightedArticlesResponse.entries.take(10).forEach {
                val weight = it.value.getOrElse("0") { "1.0" }.toFloatOrNull() ?: 1.0f
                val key = DEFAULT_BODY.replace("/?", it.key)
                weightedArticles[key] = weight
            }
        }

        if (!weightedArticles.isNullOrEmpty()) {
            MeteringPrefs.setArticleWeights(PaywallConstants.PW_ARTICLE_WEIGHTS, weightedArticles)
            MeteringPrefs.setArticleWeightsLastFetched(System.currentTimeMillis())
        }
    }

    /**
     * Pretty print Tetro response
     */
    private fun printResponse(response: TetroResponse?) {
        response?.apply {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(this)
            Logger.d(Tag, "Response - $json")
        }
    }

    /**
     * DEBUG PANEL ONLY : Used to change tetro envirnonment between prod/stage. Recreates Retrofit service.
     */
    fun resetTetroNetwork(tetroBaseUrl: String?, meteringProxyBaseUrl: String?) {
        TetroApiService.getInstance().resetTetroNetwork(tetroBaseUrl, meteringProxyBaseUrl)
        tetroRemoteService = TetroApiService.getInstance().getTetroNetwork()
    }

    companion object {
        private const val DEFAULT_BODY = "https://www.washingtonpost.com/?"
        private val Tag = TetroManager::class.simpleName
        private val defaultBody = ArticleBody(listOf(ArticleObj(DEFAULT_BODY, null)))
        private const val GIFT_TOKEN_KEY = "pwapi_token"
    }
}

/**
 * Used to manage sync state so multiple syncs aren't called.
 */
sealed class SyncState {
    object NotStarted : SyncState()
    object InProgress : SyncState()
    object Complete : SyncState()
}
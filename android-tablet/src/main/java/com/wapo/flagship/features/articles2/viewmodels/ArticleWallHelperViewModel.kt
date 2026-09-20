package com.wapo.flagship.features.articles2.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.events.Article2Events
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.OmnitureX
import com.wapo.flagship.features.articles2.paywall.WallUiEvent
import com.wapo.flagship.features.articles2.tracking.PushArticleTrackingHelperData
import com.wapo.flagship.features.map.models.MapWallType
import com.wapo.flagship.features.map.models.MapWallUiData
import com.wapo.flagship.features.preferencesapi.models.SnoozeInfo
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.ArticlePageViewTrackingData
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.MeasurementMap
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.events.GiftState
import com.washingtonpost.android.paywall.events.WallState
import com.washingtonpost.android.paywall.features.tetro.DismissType
import com.washingtonpost.android.paywall.features.tetro.Prompt
import com.washingtonpost.android.paywall.features.tetro.WebTetroResponse
import com.washingtonpost.android.paywall.features.tetro.remote.ArticleObj
import com.washingtonpost.android.paywall.newdata.model.ArticleStub
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.save.database.model.ArticleAndMetadata
import com.washingtonpost.android.save.database.model.MetadataModel
import com.washingtonpost.android.save.database.model.SavedArticleModel
import com.washingtonpost.foryou.data.RecommendationsItem
import com.washingtonpost.foryou.repo.ForYouFeedRepositoryImpl
import com.washingtonpost.foryou.viewmodel.ForYouActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class ArticleWallHelperViewModel
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val savedArticleManager: SavedArticleManager,
) : ViewModel() {
    private var job: Job? = null
    private val trackingData = MutableStateFlow(ArticlePageViewTrackingData())

    private val _articleEvent = MutableSharedFlow<Article2Events>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val articleEvent = _articleEvent.asSharedFlow()

    private val pageViewValues = ConcurrentHashMap.newKeySet<String?>()

    private val _paywallEvent: LiveEvent<WallUiEvent> = LiveEvent()
    val paywallEvent: LiveData<WallUiEvent> = _paywallEvent

    /**
     * Intended for ArticleActivity to observe current article when it is loaded.
     * ArticleStub and OmnitureX (Analytics) for article are used by ArticleActivity to determine
     * paywall status of this article after it is loaded.
     * ArticleContentFragment of loaded article will post to this event.
     */
    private val _currentPaywallArticle: LiveEvent<Pair<ArticleStub, OmnitureX?>> = LiveEvent()
    val currentPaywallArticle: LiveData<Pair<ArticleStub, OmnitureX?>> = _currentPaywallArticle

    /**
     * Dispatches true if paywall should be shown when user taps on Polly inline item. False otherwise
     */
    private val _shouldPaywallAudio: LiveEvent<Boolean> =
        LiveEvent()
    val shouldPaywallAudio: LiveData<Boolean> = _shouldPaywallAudio

    /**
     * Post event to this live-data for starting the paywall check verification when user taps on Polly inline item.
     */
    private val _checkPaywallStatusForAudio: LiveEvent<Boolean> =
        LiveEvent()
    val checkPaywallStatusForAudio: LiveData<Boolean> = _checkPaywallStatusForAudio

    /**
     * Check if user has valid gift article. */
    val isValidGift
        get() = (_paywallEvent.value as? WallUiEvent.ShowGiftWall)?.giftState == GiftState.ValidNotExpired

    /**
     * Initiates the paywall verification process when user taps on Polly inline item.
     */
    fun checkAudioPaywallStatus() {
        _checkPaywallStatusForAudio.value = true
    }

    /**
     * [shouldShow] true if to be shown, false otherwise.
     */
    fun showPaywallForAudio(shouldShow: Boolean) {
        _shouldPaywallAudio.value = shouldShow
    }

    /**
     * Post Article data (ArticleStub and Analytics-omniture) to [currentPaywallArticle] when article
     * has been loaded from feed. Notifies Articles2Activity that current article required for paywalling
     * is ready.
     */
    fun dispatchArticleForPaywall(
        article: Article2,
        originalUrl: String,
    ) {
        val articleStub =
            ArticleStub(
                title = article.title,
                arcId = article.arcId,
                url = getUrl(article.contenturl, originalUrl),
                section = article.section,
                contentRestrictionCode = article.contentRestrictionCode,
                contentSection = article.sourcesection,
                ctTags = article.tags,
                commercialNode = article.commercialnode,
                tetroAuthors = getTetroAuthors(article.omniture?.contentAuthor),
                publishedDate = article.published,
                displayDate = article.displayDate,
                contentType = article.contentType,
                tetroSubtype = article.subtype,
            )
        _currentPaywallArticle.value = Pair(articleStub, article.omniture)
    }

    /**
     * Preserves query params from original URL and appends them to updated content URL so they can be passed to Tetro.
     * Tetro uses these to recognize some promotions.
     */
    private fun getUrl(
        contentUrl: String,
        originalUrl: String,
    ): String {
        val params = originalUrl.toUri().encodedQuery
        return contentUrl
            .toUri()
            .buildUpon()
            .encodedQuery(params)
            .build()
            .toString()
    }

    private fun getTetroAuthors(omnitureFormattedList: String?): String? {
        omnitureFormattedList ?: return null
        return omnitureFormattedList.lowercase().replace(";", ",")
    }

    /**
     * Dispatch delayed paywall for current loaded article as determined in [currentPaywallArticle]
     * - passReferer -> should we call tetro with referer passed or without it.
     *      - Pass it if article is deeplinked
     *      - Don't pass it if user is coming back Regwall action
     */
    fun dispatchShowPaywallDelayed(passReferrer: Boolean = true) {
        val articleStub = currentPaywallArticle.value?.first
        val articleAnalytics = currentPaywallArticle.value?.second

        articleStub?.let {
            _paywallEvent.value =
                WallUiEvent.StartDelayedWall(
                    PaywallConstants.WallType.METERED_PAYWALL,
                    it,
                    articleAnalytics,
                    passReferrer,
                )
        }
    }

    /**
     * If a regwall is dismissed from a user action (Sign In or Sign Up)
     * we need to call tetro again to validate whether this article should be metered or paywalled.
     */
    fun handleRegwallDismiss() {
        if (_paywallEvent.value is WallUiEvent.ShowRegwall) {
            dispatchShowPaywallDelayed(false)
        }
    }

    /**
     * If paywall is currently showing after resuming subscription, check if premium user and dismiss wall
     */
    fun handlePaywallResume() {
        if ((_paywallEvent.value is WallUiEvent.ShowPaywall || _paywallEvent.value is WallUiEvent.ShowPaywallByName) &&
            PaywallService.getInstance().isPremiumUser
        ) {
            dispatchDismissPaywall()
        }
    }

    private fun dispatchDismissPaywall() {
        _paywallEvent.postValue(WallUiEvent.DismissWall)
    }

    /**
     * Used to send Gift Wall Response when the given Gift Token (pwapi_token)
     * is Expired or Invalid. Should essentially be the same as the paywall but with
     * a title indicating the token is expired/invalid.
     */
    private fun dispatchShowGiftWall(giftState: GiftState) {
        _paywallEvent.postValue(WallUiEvent.ShowGiftWall(giftState))
    }

    /**
     * Dispatch action codes from Tetro to show matching Regwall.
     */
    fun dispatchShowRegwall(
        wallName: String,
        wallType: PaywallConstants.WallType,
    ) {
        _paywallEvent.postValue(WallUiEvent.ShowRegwall(wallName, wallType))
    }

    /**
     * Dispatch action codes from Tetro to show matching Softwall.
     */
    fun dispatchShowSoftwall(
        wallName: String,
        wallType: PaywallConstants.WallType,
    ) {
        _paywallEvent.postValue(WallUiEvent.ShowSoftwall(wallName, wallType))
    }

    private fun dispatchShowMapWall(
        mapWallType: MapWallType,
        mapWallPrompt: Prompt,
    ) {
        _paywallEvent.postValue(WallUiEvent.ShowMapWall(mapWallType, mapWallPrompt))
    }

    /**
     * Used to dispatch and show paywall immediately. Usually triggered by user clicks.
     */
    fun dispatchShowPaywallNow(paywallType: PaywallConstants.WallType) {
        _paywallEvent.postValue(WallUiEvent.ShowPaywall(paywallType))
    }

    fun dispatchShowPaywallByName(paywallName: String) {
        _paywallEvent.postValue(WallUiEvent.ShowPaywallByName(paywallName))
    }

    /**
     * Stop delayed paywall timer if interrupted by dialog (ie. gift icon click)
     */
    fun dispatchStopDelayedPaywall() {
        _paywallEvent.postValue(WallUiEvent.StopDelayedWall)
    }

    /**
     * Checks if the the current native article is free content. Only applicable for native articles.
     */
    fun isCurrentNativeArticleFree() = _currentPaywallArticle.value?.first?.isFreeContent ?: false

    /**
     * Log out user and than call onComplete.
     * Common use case will be Signing in as a different user.
     */
    fun logOutUser(onComplete: () -> Unit = {}) {
        viewModelScope.launch(dispatcherProvider.io) {
            PaywallService.getInstance().logOutCurrentUser()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun stopDelayedWall() {
        // Cancel the previous article job for calling Tetro
        if (job?.isActive == true) {
            job?.cancel()
        }
    }

    /**
     * Main flow for handling metering. Refer to [https://whimsical.com/apps-tetro-integration-ii-XwzW3Zir7cFaHNAKsMgj4A]
     */
    fun getDelayedTetroMetering(
        articleStub: ArticleStub,
        paywallType: PaywallConstants.WallType = PaywallConstants.WallType.METERED_PAYWALL,
        giftToken: String? = null,
    ) {
        // Cancel the previous article job for calling Tetro
        stopDelayedWall()

        // Start Job for calling Tetro
        job =
            viewModelScope.launch(dispatcherProvider.io) {
                Logger.d(TAG, "Start 1.5s Delay")
                delay(1500L)
                // Check if we are still on current article
                if (!isActive) {
                    Logger.d(TAG, "Job NOT Active ${articleStub.url} -> Cancel Tetro Call")
                    return@launch
                }

                // Call Tetro to check what wall should be shown
                val articleObj = ArticleObj(articleStub.getTetroUrl(), System.currentTimeMillis())
                val response =
                    PaywallService.getInstance().tetroManager.getWallResponse(
                        articleObj,
                        Measurement.getABTestingVariants(applicationContext),
                        giftToken,
                    )
                Logger.d(TAG, "Tetro response returned")

                // Check if we are still on current article
                if (!isActive) {
                    Logger.d(TAG, "Job NOT Active ${articleStub.url} -> Do not Handle Tetro Call")
                    return@launch
                }

                onTetroResponse()
                val data = trackingData.value
                data.omniture?.arcId?.let {
                    pageViewValues.add(it)
                    _articleEvent.tryEmit(
                        Article2Events.Article2PageViewEvent(
                            articlePageViewTrackingData = trackingData.value
                        )
                    )
                }

                // Handle Tetro response with appropriate wall
                processWallState(response, paywallType, articleStub)
            }
    }

    /**
     * Process the wall state and dispatch the appropriate WallUIEvent.
     * Paywall Response fallback -> default paywall for that platform (Playstore or Amazon)
     * Regwall Response fallback -> Universal Regwall
     * Failed Response -> use local metering service logic (from tetro sync impl.) to determine if Paywall should be shown.
     */
    private fun processWallState(
        response: WallState,
        wallType: PaywallConstants.WallType,
        articleStub: ArticleStub? = null,
    ) {
        when (response) {
            is WallState.Paywall -> {
                val paywallFallback =
                    if (Utils.isAmazonBuild()) PAYWALL_FALLBACK_AMAZON else PAYWALL_FALLBACK_PLAYSTORE
                mapCodesToWall(
                    response.codes,
                    paywallFallback
                )?.let { dispatchShowPaywallByName(it) }
            }

            is WallState.GiftWall -> {
                if (PaywallService.getInstance().isSubscriptionPaused) {
                    dispatchShowPaywallNow(wallType)
                } else {
                    dispatchShowGiftWall(response.giftState)
                }
            }

            is WallState.Regwall -> {
                val regwall = mapCodesToWall(response.codes, REGWALL_FALLBACK)
                if (regwall != null) {
                    dispatchShowRegwall(regwall, wallType)
                } else { // fall back to paywall
                    dispatchShowPaywallNow(PaywallConstants.WallType.METERED_PAYWALL)
                }
            }

            is WallState.Softwall -> {
                val softwall = mapCodesToWall(response.codes, null)
                if (softwall != null) {
                    dispatchShowSoftwall(softwall, wallType)
                } else {
                    dispatchDismissPaywall()
                }
            }

            is WallState.MapWall -> {
                val mapWallType = mapPromptsToWall(response.prompts)
                val mapWallPrompt = response.prompts?.first()
                if (mapWallType != null && mapWallPrompt != null) {
                    dispatchShowMapWall(mapWallType, mapWallPrompt)
                }
            }

            WallState.NoWall -> {
                // No Wall will be shown
            }

            is WallState.Failed -> {
                // Note: This falls back to using the isAtLimit from MeteringService. It will
                // take the last Metering state from tetro and determine if article should be walled.
                if (PaywallService.getInstance().isAtLimit(
                        articleStub?.section ?: EMPTY,
                        articleStub,
                    )
                ) {
                    dispatchShowPaywallNow(wallType)
                }
            }
        }
    }

    /**
     * Handle the webview tetro response. Process it the same way Native Article
     * Tetro response is processed.
     */
    fun handleWebviewWall(
        url: String,
        webTetroResponse: WebTetroResponse,
    ) {
        val wallState =
            PaywallService.getInstance().tetroManager.processTetroActions(
                webTetroResponse.action,
                webTetroResponse.data.actionCodes,
                webTetroResponse.data.prompts,
            )

        if (wallState is WallState.Paywall) {
            Measurement.setPaywallArticle(url, "")
        }

        processWallState(wallState, PaywallConstants.WallType.WEBVIEW_PAYWALL)
    }

    /**
     * Return first wall blocker name that matches any action code we received from Tetro.
     * If no match, return fallback.
     */
    private fun mapCodesToWall(
        codes: List<String>?,
        fallback: String?,
    ): String? {
        codes ?: return fallback
        val wallCodeMap =
            ConfigManager.getInstance().config.paywallConf
                .metering
                ?.wallMap2
        wallCodeMap?.forEach { wall ->
            if (codes.contains(wall.code)) {
                return wall.blocker ?: fallback
            }
        }
        return fallback
    }

    private fun mapPromptsToWall(prompts: List<Prompt>?): MapWallType? {
        val prompt = prompts?.first() ?: return null
        return MapWallType.entries.firstOrNull { it.typeName == prompt.type } ?: MapWallType.FLEX
    }

    private val _scrollDepth = MutableLiveData(0)
    val scrollDepth: LiveData<Int> = _scrollDepth

    fun updateScrollDepth(depth: Int) {
        _scrollDepth.postValue(depth)
    }

    fun canShowMapWall(prompt: Prompt?): Boolean {
        try {
            updateIfStale()
        } catch (e: Exception) {
            Logger.e(TAG, "Wall dismissal stale check error: ${e.message}")
        }

        if (PrefUtils.getAllowMapOnEveryArticle(applicationContext)) {
            Logger.d(TAG, "Allow MAP on every article enabled")
            return true
        }

        val appearance = prompt?.appearance
        Logger.d(TAG, "MAP prompt appearance: $appearance")
        appearance ?: return true

        if (appearance.dismiss == DismissType.SERVER.value) {
            var shouldShowWall = true
            val wallDismissal = PrefUtils.getWallDismissal(applicationContext)?.toMutableMap()

            prompt.id?.let { id ->
                val thisWall = wallDismissal?.get(WALL_ID)?.get(id)
                val uxp = thisWall?.uxp ?: 0L
                if (uxp < System.currentTimeMillis()) {
                    wallDismissal?.put(id, null)
                }
            }

            prompt.appearance?.dismissWalls?.forEach { id ->
                val keys =
                    if (id == "current") {
                        listOf(WALL_ID, prompt.id)
                    } else {
                        id.split("-")
                    }

                if (keys.size != 2) {
                    Logger.d(
                        TAG,
                        "$id is an invalid wall-dismissal value. Should be formatted as \"key0-key1\""
                    )
                    return@forEach
                }

                val snoozeInfo = wallDismissal?.get(keys[0])?.get(keys[1]) ?: return@forEach
                val exp = snoozeInfo.exp ?: 0L
                val mht = snoozeInfo.mht ?: 0

                if (keys[1] == prompt.id && exp > System.currentTimeMillis()) {
                    shouldShowWall = false
                } else if (exp < System.currentTimeMillis()) {
                    snoozeInfo.exp = null
                }

                if (keys[1] == prompt.id && mht > 0) {
                    shouldShowWall = false
                    val newMht = mht - 1
                    snoozeInfo.mht =
                        if (newMht > 0) {
                            newMht
                        } else {
                            null
                        }
                }

                wallDismissal[keys[0]]?.set(keys[1], snoozeInfo)
            }

            wallDismissal?.let { updateLocalWallDismissalPref(it) }

            Logger.d(TAG, "MAP server shouldShowWall: $shouldShowWall")
            return shouldShowWall
        } else { // DismissType.CLIENT
            val dismissExpirationSeconds =
                if (PrefUtils.getOverrideMapSnoozeTime(applicationContext)) {
                    MAP_SNOOZE_TIME_OVERRIDE
                } else {
                    appearance.dismissExpirationSeconds ?: 0L
                }
            val lastDisplayTimeInSeconds = PrefUtils.getMapLastDisplayTime(applicationContext)
            val timeSinceLastDisplay = System.currentTimeMillis() / 1000 - lastDisplayTimeInSeconds

            val maxSnooze = appearance.maxSnooze ?: Int.MAX_VALUE
            val snoozeCount = PrefUtils.getMapSnoozeCount(applicationContext)

            val shouldShowWall =
                timeSinceLastDisplay > dismissExpirationSeconds && snoozeCount < maxSnooze
            Logger.d(TAG, "MAP client shouldShowWall: $shouldShowWall")
            return shouldShowWall
        }
    }

    fun processMapWallDismissalLogic(prompt: Prompt): Long {
        try {
            updateIfStale()
        } catch (e: Exception) {
            Logger.e(TAG, "Wall dismissal stale check error: ${e.message}")
        }
        val appearance = prompt.appearance
        Logger.d(TAG, "MAP prompt appearance: $appearance")
        appearance ?: return 0L

        if (appearance.dismiss == DismissType.SERVER.value) {
            val wallDismissal =
                PrefUtils.getWallDismissal(applicationContext)?.toMutableMap() ?: mutableMapOf()

            val dismissExpirationMilliSeconds = appearance.dismissExpirationSeconds?.times(1000)
            val dismissLifespanMilliseconds = appearance.dismissLifespanSeconds?.times(1000)
            val maxSnooze = appearance.maxSnooze

            appearance.dismissWalls?.forEach { id ->
                val keys =
                    if (id == "current") {
                        listOf(WALL_ID, prompt.id)
                    } else {
                        id.split("-")
                    }

                if (keys.size != 2) {
                    Logger.d(
                        TAG,
                        "$id is an invalid wall-dismissal value. Should be formatted as \"key0-key1\""
                    )
                    return@forEach
                }

                val snoozeInfo =
                    wallDismissal[keys[0]]?.get(keys[1]) ?: SnoozeInfo(null, null, null)

                if (dismissExpirationMilliSeconds != null) {
                    snoozeInfo.exp = System.currentTimeMillis() + dismissExpirationMilliSeconds
                }

                if (dismissLifespanMilliseconds != null) {
                    snoozeInfo.uxp = System.currentTimeMillis() +
                            max(
                                dismissExpirationMilliSeconds ?: 0L,
                                dismissLifespanMilliseconds,
                            )
                }

                if (maxSnooze != null) {
                    snoozeInfo.mht = maxSnooze
                }

                wallDismissal[keys[0]]?.set(keys[1], snoozeInfo)
            }

            updateLocalWallDismissalPref(wallDismissal)
            /* Using just (exp - current time) was resulting in a value that
               DateUtils.timePeriodString() was rounding down to 29 days, so subtracting a minute
               to avoid that, since for the most part the snooze will be 1 month. */
            val oneMinuteAgo =
                System.currentTimeMillis() - PaywallConstants.ONE_MINUTE_IN_MILLISECONDS
            val snoozeDuration =
                wallDismissal[WALL_ID]?.get(prompt.id)?.exp?.minus(oneMinuteAgo) ?: 0L
            Logger.d(TAG, "MAP server snooze duration: $snoozeDuration")
            return snoozeDuration
        } else { // DismissType.CLIENT
            PrefUtils.updateMapSnoozeCount(applicationContext)
            PrefUtils.setMapLastDisplayTime(
                applicationContext,
                System.currentTimeMillis() / 1000,
            )
            val snoozeDuration = prompt.appearance?.dismissExpirationSeconds?.times(1000) ?: 0L
            Logger.d(TAG, "MAP server snooze duration: $snoozeDuration")
            return snoozeDuration
        }
    }

    fun trackMapWallDismissal(trackingInfo: OmnitureX?) {
        trackingInfo?.let {
            Measurement.trackMapDismissal(
                it.pageName,
                it.arcId,
                it.contentType,
                it.channel,
                it.subSection,
                it.authorId,
            )
        }
    }

    private fun updateLocalWallDismissalPref(wallDismissal: Map<String, Map<String, SnoozeInfo?>?>?) {
        PrefUtils.setWallDismissal(
            applicationContext,
            wallDismissal,
        )
        PrefUtils.setWallDismissalLmt(
            applicationContext,
            System.currentTimeMillis(),
        )
    }

    fun isMapRecommendationSaved(url: String?): Boolean =
        url?.let {
            savedArticleManager.getArticleByUrl(it)
        } != null

    private fun updateIfStale() {
        if (PreferencesSyncCoordinator.WALL_DISMISSAL in PreferencesSyncCoordinator.stale(
                applicationContext
            ) && !PreferencesSyncCoordinator.isRunning()
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    FlagshipApplication.getInstance().wallDismissalRepo.getWallDismissal()
                } catch (e: Exception) {
                    Logger.e(TAG, "Wall dismissal refresh failed: ${e.message}")
                }
            }
        }
    }

    fun saveOrRemoveMapRecommendation(
        mapWallUiData: MapWallUiData,
        currentlySaved: Boolean,
        recommendationsItem: RecommendationsItem?,
    ) {
        if (currentlySaved) {
            val savedArticleMeta = mapWallUiData.url?.let { ArticleAndMetadata(0L, it) }
            savedArticleMeta?.let { savedMeta ->
                savedArticleManager.removeArticles(listOf(savedMeta))
                Measurement.trackSaveUnsave(
                    "trackingInfo?.pageName",
                    recommendationsItem?.arcId,
                    "trackingInfo?.contentType",
                    null,
                    false,
                    false,
                    true,
                    savedMeta.contentURL,
                )
            }
        } else {
            mapWallUiData.url?.let { url ->
                val model =
                    SavedArticleModel(
                        url,
                        System.currentTimeMillis()
                    )

                val metadataModel =
                    MetadataModel(
                        url,
                        System.currentTimeMillis()
                    ).also {
                        it.imageURL = mapWallUiData.imageUrl
                        it.headline = mapWallUiData.title
                        it.byline = mapWallUiData.byline
                    }

                savedArticleManager.addArticle(model, metadataModel)
                Measurement.trackSaveUnsave(
                    null,
                    null,
                    null,
                    null,
                    false,
                    true,
                    true,
                    null,
                )
            }
        }
    }

    fun fetchNewMapArticle(
        forYouActivityViewModel: ForYouActivityViewModel,
        isPushOriginated: Boolean,
        currentPageUrl: String?,
        recommendNew: Boolean,
    ) {
        forYouActivityViewModel.fetchData(
            ForYouFeedRepositoryImpl.SURFACE_RECIRC_SOFTWALL,
            isPushOriginated,
            1,
            currentPageUrl,
            recommendNew,
        )
    }

    /**
     * Build Tetro URL with following query params
     * pwapi_contentsection <-- Value of sourcesection from RDS
     * pwapi_ct_tags <-- Value of tags from RDS
     * pwapi_sct_tags <-- Constant value default
     * commercial_node <-- Value of commercialnode from RDS
     * content_type <-- Constant value article
     * env <-- Constant value app
     * referrer <-- deeplinking from another referrer
     * tetroUtm <-- type of the source (ie Social)
     * tetro_authors <-- Authors of article
     * published_date <-- Published date of article
     * display_date <-- Display date of article
     */
    private fun ArticleStub.getTetroUrl(): String {
        val builder =
            Uri
                .parse(this.url)
                .buildUpon()
                .appendQueryParameter(SCT_TAGS_KEY, SCT_TAGS_VALUE)
                .appendQueryParameter(ENV_KEY, ENV_VALUE)

        this.contentSection?.let {
            builder.appendQueryParameter(CONTENT_SECTION_KEY, it)
        }

        this.ctTags?.let {
            builder.appendQueryParameter(CT_TAGS_KEY, it)
        }

        this.commercialNode?.let {
            builder.appendQueryParameter(COMMERCIAL_NODE_KEY, it)
        }

        this.referrer?.let {
            builder.appendQueryParameter(REFERRER_KEY, it)
        }

        this.tetroUtm?.let {
            builder.appendQueryParameter(TETRO_UTM_KEY, it)
        }

        this.tetroAuthors?.let {
            builder.appendQueryParameter(TETRO_AUTHORS_KEY, it)
        }

        this.publishedDate?.let {
            builder.appendQueryParameter(PUBLISHED_DATE_KEY, it.toString())
        }

        this.displayDate?.let {
            builder.appendQueryParameter(DISPLAY_DATE_KEY, it.toString())
        }

        this.contentType?.let {
            builder.appendQueryParameter(CONTENT_TYPE_KEY, it)
        }

        this.tetroSubtype?.let {
            builder.appendQueryParameter(TETRO_SUBTYPE_KEY, it)
        }

        this.arcId?.let {
            builder.appendQueryParameter(ARC_ID, it)
        }

        return builder.build().toString()
    }

    fun trackPageView(
        omnitureData: TrackingInfo,
        positionData: Int,
        currentAppTabData: String,
        currentAppSectionData: String,
        pushTrackingHelperData: PushArticleTrackingHelperData?,
        measurementMap: MeasurementMap) {
        trackingData.update { current ->
            current.copy(
                omniture = omnitureData,
                position = positionData,
                currentAppTab = currentAppTabData,
                currentAppSection = currentAppSectionData,
                pushTrackingHelperData = pushTrackingHelperData,
                measurementMap = measurementMap,
            )
        }
    }

    fun onTetroResponse() {
        if (hasPendingData()) {
            trackingData.update { current ->
                current.copy(
                    omniture = current.omniture?.apply {
                        tetroAction = PaywallService.getInstance().currentTetroAction.toString()
                        actionCode = PaywallService.getInstance().currentTetroActionCodes
                    }
                )
            }
        }
    }

    private fun hasPendingData(): Boolean = trackingData.value.omniture != null && trackingData.value.position >= 0

    enum class MapDismissOrigin {
        SYSTEM,
        CLOSE_BUTTON,
        MENU_SNOOZE,
    }

    companion object {
        private const val CONTENT_SECTION_KEY = "pwapi_contentsection"
        private const val CT_TAGS_KEY = "pwapi_ct_tags"
        private const val SCT_TAGS_KEY = "pwapi_sct_tags"
        private const val COMMERCIAL_NODE_KEY = "commercial_node"
        private const val CONTENT_TYPE_KEY = "content_type"
        private const val ENV_KEY = "env"
        private const val TETRO_UTM_KEY = "tetro_utm"
        private const val REFERRER_KEY = "referrer"
        private const val TETRO_AUTHORS_KEY = "tetro_authors"
        private const val PUBLISHED_DATE_KEY = "published_date"
        private const val DISPLAY_DATE_KEY = "display_date"
        private const val TETRO_SUBTYPE_KEY = "tetro_subtype"

        private const val PAYWALL_FALLBACK_PLAYSTORE = PaywallConstants.WALL_NAME_MAIN
        private const val PAYWALL_FALLBACK_AMAZON = PaywallConstants.WALL_NAME_AMAZON_MAIN
        private const val REGWALL_FALLBACK = "regwall"
        private const val ARC_ID = "arcid"

        private const val SCT_TAGS_VALUE = "default"
        private const val ENV_VALUE = "app"

        private const val WALL_ID = "wallId"

        private const val EMPTY = ""

        private const val MAP_SNOOZE_TIME_OVERRIDE = 30L

        private val TAG = ArticleWallHelperViewModel::class.java.simpleName
    }
}

/*
 * Copyright (C) 2021 Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship

import android.app.Activity
import android.app.Application
import android.app.job.JobScheduler
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.captechconsulting.captechbuzz.model.images.BitmapLruImageCache
import com.jakewharton.threetenabp.AndroidThreeTen
import com.urbanairship.Autopilot
import com.wapo.adsinf.AdManager
import com.wapo.adsinf.interfaces.IAdProvider
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.adsinf.tracking.IAdTracker
import com.wapo.android.commons.appsFlyer.AppsFlyer
import com.wapo.android.commons.appsFlyer.AppsFlyer.init
import com.wapo.android.commons.appsFlyer.AppsFlyer.updateServerUninstallToken
import com.wapo.android.commons.config.sec.helper.SSLSocketFactoryProvider
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.engagement.EngagementTracker
import com.wapo.android.commons.engagement.SessionTracker
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.TLSSocketFactory
import com.wapo.android.commons.util.getHourOfDay
import com.wapo.android.push.FCMPushNotificationReceiverInterface
import com.wapo.android.push.PushNotification
import com.wapo.android.push.PushService
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.config.ConfigUtils
import com.wapo.flagship.content.AlertsSettingsImpl
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.content.ContentUpdateRulesManager
import com.wapo.flagship.content.image.ImageService
import com.wapo.flagship.data.ArchiveManager
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.data.CacheManagerImpl
import com.wapo.flagship.external.foryouwidget.data.ForYouWidgetMapper
import com.wapo.flagship.external.foryouwidget.data.updateWidgetRecommendations
import com.wapo.flagship.external.foryouwidget.workers.ForYouWidgetUpdateWorker
import com.wapo.flagship.features.articles.recirculation.RecirculationStorage
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.ask.repo.AskQuestionsRepo
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import com.wapo.flagship.features.deeplinks.AirshipDeepLinkListener
import com.wapo.flagship.features.deeplinks.AirshipInAppMessageExtender
import com.wapo.flagship.features.deeplinks.AirshipInAppMessageListener
import com.wapo.flagship.features.deeplinks.AirshipUrlAllowListCallback
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.deeplinks.OneLinkListener
import com.wapo.flagship.features.habittiles.HabitTilesApiDataListener
import com.wapo.flagship.features.mypost.FollowProviderImpl
import com.wapo.flagship.features.mypost.SaveProviderImpl
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.nightmode.NightModeController
import com.wapo.flagship.features.nightmode.NightModeManager
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.onetrust.OneTrustHelper.initializationState
import com.wapo.flagship.features.onetrust.OneTrustHelper.isEURegion
import com.wapo.flagship.features.onetrust.OneTrustHelper.startOrStopExternalLibrariesTracking
import com.wapo.flagship.features.onetrust.OneTrustInitializationState
import com.wapo.flagship.features.personalizedpodcasts.repo.PersonalizedPodcastRepository
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.VideoManager
import com.wapo.flagship.features.posttv.VideoManager2
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.preferencesapi.repo.CheckPrefRepo
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.wapo.flagship.features.preferencesapi.repo.NewsprintRepo
import com.wapo.flagship.features.preferencesapi.repo.WallDismissalRepo
import com.wapo.flagship.features.print.PrintSyncReceiver
import com.wapo.flagship.features.purchasedarticles.repo.PurchasedArticleManager
import com.wapo.flagship.features.sections.SectionApplication
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.support.ZendeskProviderImpl
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.features.wpvideos.repo.WatchVideosRepository
import com.wapo.flagship.lifecycle.AppFragmentLifecycleCallbacks
import com.wapo.flagship.lifecycle.LegacyActivityLifecycleCallbacks
import com.wapo.flagship.lifecycle.LegacyActivityLifecycleCallbacksAPI21
import com.wapo.flagship.lifecycle.MusicLifecyclerObserver
import com.wapo.flagship.lifecycle.OneTrustLifecycleObserver
import com.wapo.flagship.lifecycle.PaywallLifecycleObserver
import com.wapo.flagship.lifecycle.SyncLifecycleObserver
import com.wapo.flagship.lifecycle.VideoLifecycleObserver
import com.wapo.flagship.network.SimpleSSLSocketFactory
import com.wapo.flagship.push.PushListener
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.roomdb.AppDatabase
import com.wapo.flagship.sdk.iterable.IterableSdk
import com.wapo.flagship.sync.SyncManager
import com.wapo.flagship.util.AppVolleyConnector
import com.wapo.flagship.util.ChartbeatManager.init
import com.wapo.flagship.util.ChartbeatManager.setUserAnonymous
import com.wapo.flagship.util.ChartbeatManager.setUserLoggedIn
import com.wapo.flagship.util.ChartbeatManager.setUserPaid
import com.wapo.flagship.util.ComScoreHelper.init
import com.wapo.flagship.util.ComScoreHelper.isInitialized
import com.wapo.flagship.util.FlagshipPaywallConnector
import com.wapo.flagship.util.JUcidTracker
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.Util
import com.wapo.flagship.util.WPUrlAnalyser
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.wapo.flagship.util.tracking.AdTrackerImpl
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.PaywallOmniture
import com.wapo.flagship.features.deeplinks.OneLinkRepository
import com.wapo.flagship.kmp.core.DependencyRegistry
import com.wapo.flagship.wrappers.CrashWrapper
import com.wapo.text.TypefaceCache
import com.wapo.view.tooltip.TooltipPopupManager
import com.wapo.zendesk.ZendeskApplication
import com.wapo.zendesk.ZendeskProvider
import com.wapo.zendesk.notification.ZendeskApplicationObserver
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.Source
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig
import com.washingtonpost.android.follow.helper.FollowApplication
import com.washingtonpost.android.follow.helper.FollowManager
import com.washingtonpost.android.follow.helper.FollowProvider
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthApplication
import com.washingtonpost.android.paywall.billing.AbstractBillingActivity
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper
import com.washingtonpost.android.paywall.billing.StoreHelperCreator
import com.washingtonpost.android.paywall.features.ccpa.getCCPAAdsPrivacyString
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper
import com.washingtonpost.android.save.SavedArticleManager
import com.washingtonpost.android.volley.RequestQueue
import com.washingtonpost.android.volley.VolleyConnector
import com.washingtonpost.android.volley.VolleyConnectorProvider
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.GlobalImageListener
import com.washingtonpost.foryou.domain.ForYouFeedRepository
import com.washingtonpost.foryou.domain.HabitTilesRepository
import com.washingtonpost.userhistory.domain.UserHistoryManager
import com.washpost.airship.AirshipProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
open class FlagshipApplication :
    Application(),
    PostTvApplication,
    AuthApplication,
    SectionApplication,
    FCMPushNotificationReceiverInterface,
    FollowApplication,
    NightModeController,
    LifecycleObserver,
    ZendeskApplication,
    VolleyConnectorProvider {

    @Inject
    lateinit var repo: Articles2Repository

    @Inject
    lateinit var articleDatabase: AppDatabase

    @Inject
    lateinit var purchasedArticleManager: PurchasedArticleManager

    @Inject
    lateinit var newslettersRepository: NewslettersRepository

    @Inject
    lateinit var contentPacksRepo: ContentPacksRepo

    @Inject
    lateinit var checkPrefRepo: CheckPrefRepo

    @Inject
    lateinit var wallDismissalRepo: WallDismissalRepo

    @Inject
    lateinit var newsprintRepo: NewsprintRepo

    @Inject
    lateinit var forYouFeedRepo: ForYouFeedRepository

    @Inject
    lateinit var habitTilesRepo: HabitTilesRepository

    @Inject
    lateinit var habitTilesApiDataListener: HabitTilesApiDataListener

    @Inject
    lateinit var coroutineScopeProvider: CoroutineScopeProvider

    @Inject
    lateinit var askQuestionsRepo: AskQuestionsRepo

    @Inject
    lateinit var userHistoryManager: UserHistoryManager

    @Inject
    lateinit var iterableSdk: IterableSdk

    @Inject
    lateinit var watchVideoRepository: WatchVideosRepository

    @Inject
    lateinit var personalizedPodcastRepository: PersonalizedPodcastRepository

    @Inject
    lateinit var cacheManager: CacheManager

    @Inject
    lateinit var contentManager: ContentManager

    @Inject
    lateinit var contentUpdateRulesManager: ContentUpdateRulesManager

    @Inject
    lateinit var animatedImageLoader: AnimatedImageLoader

    @Inject
    lateinit var requestQueue: RequestQueue

    @Inject
    lateinit var adService: AdService

    @Inject
    lateinit var oneLinkRepository: OneLinkRepository

    private val config get() = ConfigManager.getInstance().config


    @Inject
    lateinit var kmpDependencyRegistry: DependencyRegistry

    val archiveManager: ArchiveManager by lazy { ArchiveManager(this) }

    val nightModeManager: NightModeManager by lazy { NightModeManager(this) }

    val articleRecircCarouselCache: RecirculationStorage by lazy {
        RecirculationStorage(
            contentManager,
        )
    }

    private val appVideoManager: VideoManager by lazy { VideoManager(this) }

    private val appVideoManager2: VideoManager2 by lazy { VideoManager2(this) }

    lateinit var imageService: ImageService
        private set

    val savedArticleManager: SavedArticleManager by lazy {
        SavedArticleManager.getInstance(SavedArticleManager.Params(SaveProviderImpl(applicationContext)))
    }

    val followManager: FollowManager by lazy {
        FollowManager.getInstance(this)
    }

    lateinit var paywallOmniture: PaywallOmniture
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var alertsSettings: AlertsSettings
        private set

    lateinit var musicServiceConnection: MusicServiceConnection
        private set

    private val adTrackerImpl: IAdTracker by lazy { AdTrackerImpl() }

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var activityCallbacks: LegacyActivityLifecycleCallbacks
    private lateinit var fragmentCallbacks: AppFragmentLifecycleCallbacks
    private val canMakePostPaywallInitVerifyCalls: Boolean = true
    private val canRunSync: Boolean = true
    private var shouldSuppressPostPaywallInitVerifyCalls: Boolean = false
    private var _offlineTill: Long = 0
    private var connector: FlagshipPaywallConnector? = null
    var assignedImageServiceConfig: ImageServiceConfig? = null
        private set
    var isActivityPrintRelated: Boolean = false

    /**
     * Clears orphaned JobScheduler jobs if the count approaches the OS limit.
     *
     * Background: WorkManager's ExistingWorkPolicy.REPLACE and Airship SDK's internal workers
     * accumulate orphaned jobs in the OS JobScheduler over time. These jobs persist across
     * app sessions but are not tracked in WorkManager's database. When the count hits the OS
     * limit (100 on API <= 30, 150 on API >= 31), WorkManager's ForceStopRunnable crashes on
     * the next cold launch with IllegalStateException.
     *
     * This cleanup runs before WorkManager initializes (attachBaseContext precedes onCreate),
     * which is exactly where the crash occurs. After cancelAll(), WorkManager reschedules its
     * DB-tracked jobs on init — only the orphaned jobs are permanently cleared.
     *
     * Thresholds: 90 for API < 31 (100 job limit), 140 for API >= 31 (150 job limit).
     *
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val jobScheduler = base.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
        if (jobScheduler != null) {
            val pendingJobs = try { jobScheduler.allPendingJobs } catch (e: Exception) { return }
            val threshold = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 140 /* OS JobScheduler limit: 150 */ else 90 /* OS JobScheduler limit: 100 */
            if (pendingJobs.size >= threshold) {
                try {
                    jobScheduler.cancelAll()
                } catch (e: Exception) { }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appInstance = this
        initSSLSocketFactoryProvider()

        AppContext.setIsAppUpgrade(applicationContext)  //call this before version code is set in pref in ConfigService
        AppContextUtils.init(
            applicationContext,
            Measurement.detectAppName()
        )       //  Call this before making any config request (AppContextUtils.init initializes the required appApiUserAgent for remote requests)
        ConfigUtils.initConfig(applicationContext, kmpDependencyRegistry)
        AppContext.init(applicationContext)
        nightModeManager.handleSystemNightMode(resources.configuration.uiMode)
        AndroidThreeTen.init(this)
        init()
    }

    @OptIn(UnstableApi::class)
    private fun init() {
        WPUrlAnalyser.init()
        TypefaceCache.configure(Util.isFirePhone())
        _offlineTill = 0
        connector =
            FlagshipPaywallConnector(
                this,
                resources.getString(R.string.paywallStoreType),
                resources.getString(R.string.paywallStoreEnv),
            )
        paywallOmniture = PaywallOmniture()

        initExteriorLibs()

        // ExoPlayer Cache initialization for Video files with 64MB size.
        ExoPlayerCache.init(this, cacheDir, (64 * 1024 * 1024).toLong())

        val bitmapImageCache = BitmapLruImageCache(getCacheSize())
        val globalImageListener: GlobalImageListener =
            object : GlobalImageListener {
                override fun onResponse(
                    data: Any?,
                    requestUrl: String?,
                    isImmediate: Boolean,
                ) {
                }

                override fun onErrorResponse(
                    requestUrl: String,
                    error: VolleyError,
                ) {
                    Logger.e(TAG, "Failed to download the image for URL $requestUrl", error)
                }
            }

        // Determine the correct bundle for the first time
        assignedImageServiceConfig = selectAssignedImageServiceConfig()
        if (AppContext.isFirstRun() || AppContext.getMetaDbVersion() < CacheManagerImpl.METADATA_DB_VERSION) {
            AppContext.setMetaDbVersion(CacheManagerImpl.METADATA_DB_VERSION)
        }

        alertsSettings = AlertsSettingsImpl()

        musicServiceConnection = MusicServiceConnection.getInstance()

        imageService = ImageService(animatedImageLoader, requestQueue)
        PrintSyncReceiver.setAlarm(this)
        contentUpdateRulesManager.clearTimes()
        syncManager =
            SyncManager(this).apply {
                removeSyncAccount()
            }

        val lifeCycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifeCycle.addObserver(OneTrustLifecycleObserver(lifeCycle))
        lifeCycle.addObserver(SyncLifecycleObserver(lifeCycle))
        lifeCycle.addObserver(VideoLifecycleObserver(lifeCycle))
        lifeCycle.addObserver(ZendeskApplicationObserver(this))
        lifeCycle.addObserver(MusicLifecyclerObserver(lifeCycle))

        fragmentCallbacks = AppFragmentLifecycleCallbacks()
        activityCallbacks =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LegacyActivityLifecycleCallbacksAPI21(fragmentCallbacks, iterableSdk)
            } else {
                LegacyActivityLifecycleCallbacks(fragmentCallbacks, iterableSdk)
            }
        registerActivityLifecycleCallbacks(activityCallbacks)
        lifeCycle.addObserver(
            PaywallLifecycleObserver(
                lifeCycle,
                forYouFeedRepo,
                newslettersRepository,
                contentManager,
                coroutineScopeProvider,
            ),
        )

        startOrStopExternalLibrariesTracking()
    }

    private fun initExteriorLibs() {
        CrashWrapper.init()
        CrashWrapper.setUserIdentifier(DeviceUtils.getUniqueDeviceId(applicationContext))
        Measurement.configureAppMeasurement(this)
        initEngagement()

        // Init appsFlyer
        val oneLinkListener = OneLinkListener()
        WapoSecDataProvider.appsFlyerKey?.let {
            init(AppsFlyer.Config(it, true, oneLinkListener))
        } ?: CrashWrapper.sendException(RuntimeException("Null AppsFlyer SDK dev key"))

        observeOneTrustSdkStartedStatus()

        // Initialize Chartbeat
        val chartbeatConfig = config.chartbeatConfig
        init(chartbeatConfig.accountId, chartbeatConfig.domain, this)

        // Init Tooltip
        TooltipPopupManager.get(this).setTooltipListener(AppTooltipListener())

        // Initialize Push
        initializePush()

        //  Init Ads
        AdManager.init(object : IAdProvider {
            override val applicationContext: Context
                get() = this@FlagshipApplication.applicationContext
            override val isDebugBuild: Boolean
                get() = AppContextUtils.isDebugBuild()
            override val ccpaAdsPrivacyString: String
                get() = getCCPAAdsPrivacyString(applicationContext)
            override val isEURegion: Boolean
                get() = isEURegion()
            override val isTestAdsEnabled: Boolean
                get() = AppPreferences.isTestAdsEnabled()
            override val testAdsValue: String
                get() = AppPreferences.getTestAdsValue()
            override val appVersionName: String
                get() = BuildConfig.VERSION_NAME
            override val adSubscriptionStatus: String
                get() = PaywallService.getConnector().adSubscriptionStatus
            override val jUcid: String
                get() = JUcidTracker.jUcid
            override val adTracker: IAdTracker
                get() = adTrackerImpl
            override val hourOfDay: Int
                get() = getHourOfDay()

            override fun getCCPABundle(): Bundle? =
                com.washingtonpost.android.paywall.features.ccpa.getCCPABundle()

            override fun getSectionsAdTargetingValues(
                primarySectionId: String,
                id: String?,
                adCall: Any?,
                permutive: Any?,
                contentUrl: String?
            ): Map<String, List<String>> =
                com.wapo.flagship.common.getSectionsAdTargetingValues(
                    primarySectionId,
                    TargetingContent(id, adCall, permutive),
                    contentUrl
                )
        })
    }

    private fun initSSLSocketFactoryProvider() {
        SSLSocketFactoryProvider.sslSocketFactory = try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) TLSSocketFactory() else SimpleSSLSocketFactory()
        } catch (e: Exception) {
            Logger.e(TAG, Utils.exceptionToString(e))
            null
        }
    }

    private fun initEngagement() {
        EngagementTracker.init {
            Measurement.trackEngagement(it)
        }
        SessionTracker.init(
            applicationContext,
            EngagementTracker.getInstance(),
            remoteLog = {
                EventLog.Builder()
                    .apply {
                        setMessage(it)
                        setModule(LogModules.ENGAGEMENT_METRICS)
                    }.run {
                        RemoteLog.e(applicationContext, build())
                    }
            }
        )
    }

    private fun initializePush() {
        // Initialize PushService
        PushService.init(
            applicationContext,
            AirshipProvider,
            config.airshipPushConfig,
            PushListener { sendRTEPushData(it) }
        ).also {
            iterableSdk.attachIamProvider(PushService.getInstance())
        }
        // Initialize Airship
        AirshipProvider.apply {
            deepLinkListener = AirshipDeepLinkListener(DeepLinksProcessor)
            urlAllowListCallback = AirshipUrlAllowListCallback()
            inAppMessageListener = AirshipInAppMessageListener(applicationContext)
            inAppMessageExtender = AirshipInAppMessageExtender()
            urlAllowList = config.airshipConfig.openURLPatterns
        }
        // TakeOff Airship
        Autopilot.automaticTakeOff(this)
        // Handle topics migration or config topics
        PushPreferencesHelper.onAppRun(applicationContext, AppContext.isFirstRun())
        // Create a Default channel for Notifications
        val mNotificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    PushListener.DEFAULT_CHANNEL_ID,
                    PushListener.DEFAULT_CHANNEL_TITLE,
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            mNotificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Sends Real-Time Event (RTE) push data for tracking and analytics.
     *
     * - Parses the URL from the push notification.
     * - Extracts metadata such as push ID, category/type, and test group.
     * - posts the push event to the ViewModel..
     */

    private fun sendRTEPushData(pushData: PushNotification?) {
        coroutineScopeProvider.sync.launch {
            pushData?.let { notification ->
                val url = notification.url
                val pushId = notification.pushID
                val pushCategory = notification.type
                val testGroup = Measurement.getTestGroupString(notification.testGroups)
                val loginId =
                    PaywallPrefHelper.getInstance(applicationContext).getLoginId(applicationContext)

                userHistoryManager.postPushEvent(
                    url,
                    pushId,
                    pushCategory,
                    testGroup,
                    false,
                    loginId
                )
            }
            setPushData(pushData)
        }
    }

    private fun setPushData(pushData: PushNotification?) {
        pushData?.let {
            PrefUtils.setPushData(
                this,
                pushData.type,
                Measurement.getTestGroupString(pushData.testGroups),
                pushData.url,
                pushData.pushID
            )
        }
    }

    private fun selectAssignedImageServiceConfig(): ImageServiceConfig {
        val screenWidth: Int = resources.displayMetrics.widthPixels
        return ImageServiceConfig(screenWidth, Int.MAX_VALUE)
    }

    @get:Synchronized
    val isOnline: Boolean
        get() = System.currentTimeMillis() > _offlineTill

    @Synchronized
    fun reportNetworkError(timestamp: Long) {
        _offlineTill = timestamp + QUARANTINE_INTERVAL_MILLIS
    }

    fun canRunSync(): Boolean = canRunSync

    fun updateConfigsAndInitPaywall() {
        ConfigManager.getInstance().updateConfig(source = Source.REMOTE)
        val storeBillingHelper: AbstractStoreBillingHelper = StoreHelperCreator.create()
        PaywallService.initialize(
            this,
            config.paywallConfig,
            "Vk1qzbBQbUUR7ICcYwAFFrNQv55TAbc2",
            connector,
            paywallOmniture,
            storeBillingHelper,
        )
        // Moving it here as this particular measurement value needs PaywallService to be initialized
        val paywallService: PaywallService? = PaywallService.getInstance()
        if (paywallService != null) {
            PaywallReactive.updateSubAttributes(
                PaywallService.getConnector().paywallSubAttributes
            )

            if (paywallService.loggedInUser != null) {
                Measurement.setUUIDInDefaultMap(paywallService.loggedInUser.uuid)
            }
            paywallService.initializeIdentityPreferencesWithDefaults()
        }
        makePostInitializeVerifyCalls()
    }

    fun makePostInitializeVerifyCalls() {
        if (currentActivity is PaywallVerifyCallsSuppressHelper) {
            val helper: PaywallVerifyCallsSuppressHelper =
                currentActivity as PaywallVerifyCallsSuppressHelper
            if (helper.isActivityLoadingPushAlert()) {
                if (!shouldSuppressPostPaywallInitVerifyCalls) {
                    shouldSuppressPostPaywallInitVerifyCalls =
                        helper.shouldSuppressPostPaywallInitVerifyCalls()
                }
            }
        }
        Logger.d(
            TAG,
            "updateConfigsAndInitPaywall shouldSuppressPostPaywallInitVerifyCalls $shouldSuppressPostPaywallInitVerifyCalls",
        )
        if (canMakePostPaywallInitVerifyCalls && !shouldSuppressPostPaywallInitVerifyCalls) {
            Logger.d(TAG, "updateConfigsAndInitPaywall makePostInitializeVerifyCalls")
            PaywallService.makePostInitializeVerifyCalls()
        }
    }

    fun setShouldSuppressPostPaywallInitVerifyCalls(shouldSuppressPostPaywallInitVerifyCalls: Boolean) {
        this.shouldSuppressPostPaywallInitVerifyCalls = shouldSuppressPostPaywallInitVerifyCalls
    }

    /**
     * Helper to find if the Application is in background from any activity.
     */
    val isApplicationInBackground: Boolean
        get() =
            !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
                Lifecycle.State.STARTED,
            )

    override fun getSystemService(name: String): Any? {
        if ((CONNECTIVITY_SERVICE == name)) {
            if (!::connectivityManager.isInitialized) {
                connectivityManager = super.getSystemService(name) as ConnectivityManager
            }
            return connectivityManager
        }
        return super.getSystemService(name)
    }

    override fun getVideoManager(): VideoManager = appVideoManager

    override fun getVideoManager2(): VideoManager2 = appVideoManager2

    override fun releaseVideoManager() {
        appVideoManager.release()
    }

    /**
     * Method to release all videos (except PiP)
     */
    override fun releaseVideoManager2() {
        appVideoManager2.releaseAllVideos()
    }

    /**
     * Method to reset all videos. It releases user initiated videos and
     * mutes autoplay and looping videos.
     */
    override fun resetVideoManager2() {
        appVideoManager2.resetAllVideos()
    }

    fun releaseMusicService() {
        musicServiceConnection.release()
    }

    override fun getCurrentActivity(): Activity? = activityCallbacks.getCurrentActivity()

    val isBillingActivity: Boolean
        get() {
            return currentActivity is AbstractBillingActivity
        }

    override fun logPostTvError(eventLogBuilder: EventLog.Builder) {
        eventLogBuilder
            .apply {
                setModule(LogModules.VIDEO)
            }.run {
                RemoteLog.e(applicationContext, build())
            }
    }

    override fun logVideoAdError(errorLog: String) {
        EventLog
            .Builder()
            .apply {
                setMessage(errorLog)
                setModule(LogModules.VIDEO)
            }.run {
                RemoteLog.e(applicationContext, build())
            }
    }

    override fun pausePIP() {
        sendBroadcast(
            Intent(VideoActivity.ACTION_MEDIA_CONTROL)
                .putExtra(VideoActivity.EXTRA_CONTROL_TYPE, VideoActivity.CONTROL_TYPE_PAUSE),
        )
    }

    override fun shouldUseLegacyPlayer(): Boolean = false

    override fun shouldSuppressAds(): Boolean = adService.currentAdsMode is AdsModel.Disabled

    override fun shouldUseCustomTab(): Boolean = true

    override fun getAuthBrowserPackageName(): String? {
        return null // already using custom tabs, should remove this method eventually
    }

    override fun getAppRedirectScheme(): String = BuildConfig.APP_AUTH_REDIRECT_URI

    override fun onNewToken(token: String) {
        updateServerUninstallToken(this, token)
    }

    fun updateUserTrackingForChartbeat() {
        if (PaywallService.getInstance().isPremiumUser) {
            setUserPaid()
        } else if (PaywallService.getInstance().isWpUserLoggedIn) {
            setUserLoggedIn()
        } else {
            setUserAnonymous()
        }
    }

    override fun handleNightMode(activity: AppCompatActivity?) {
        nightModeManager.handleSystemNightMode(resources.configuration.uiMode)
        if (activity != null && !activity.isFinishing) {
            val decorView: View = activity.window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // xml windowLightNavigationBar is only supported on 27+, but the flag is supported on 26+,
                // this results in the color being incorrect on 26.
                if (!isNightModeEnabled()) {
                    decorView.systemUiVisibility = (
                            decorView.systemUiVisibility
                                    or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                            )
                } else {
                    decorView.systemUiVisibility = (
                            decorView.systemUiVisibility
                                    and WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS.inv() and
                                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                            )
                }
            }
        }
    }

    override fun isNightModeEnabled(): Boolean = nightModeManager.immediateNightModeStatus

    override val followProvider: FollowProvider
        get() {
            return FollowProviderImpl()
        }
    override val zendeskProvider: ZendeskProvider
        get() {
            return ZendeskProviderImpl()
        }

    private fun observeOneTrustSdkStartedStatus() {
        initializationState.observe(ProcessLifecycleOwner.get()) { started: OneTrustInitializationState ->
            Logger.d(TAG, "observeOneTrustSdkStartedStatus, started=${started.name}")
            // Initialize ComScore for non EU regions.
            if (!isInitialized() && !isEURegion()) {
                init(this)
            }

            // Initialize A9 ads for non EU regions.
            if (!isEURegion()) {
                if (!AdManager.isA9Initialized) {
                    AdManager.initializeA9Ads(this)
                    AdManager.isA9Initialized = true
                }
            } else {
                AdManager.isA9Initialized = false
            }
        }
    }

    fun runWidgetInit(context: Context) {
        ForYouWidgetUpdateWorker.scheduleAllUpdates(this)
        coroutineScopeProvider.sync.launch {
            try {
                Logger.d("FlagshipApp", "Running initial widget update...")
                val repository = forYouFeedRepo
                repository.makeWidgetCall()

                val cachedRecommendations = repository.getCachedArticlesPage(0)
                val widgetItems = ForYouWidgetMapper.mapToWidgetItems(cachedRecommendations)
                if (cachedRecommendations.isNotEmpty()) {
                    updateWidgetRecommendations(
                        context = context,
                        recommendations = widgetItems,
                        pageIndex = 0,
                        articleIndex = 0
                    )
                    Logger.d(TAG, "Initial widget update completed")
                }
            } catch (e: Exception) {
                Logger.e("FlagshipApp", "Failed initial widget update", e)
            }
        }
    }

    companion object {
        private val TAG: String = FlagshipApplication::class.java.simpleName
        private const val QUARANTINE_INTERVAL_MILLIS: Int = 20000
        const val ACTION_SAVE: String = "com.wapo.flagship.action.ACTION_SAVE"
        const val ACTION_SHARE: String = "com.wapo.flagship.action.ACTION_SHARE"
        const val ACTION_DELETE: String = "com.wapo.flagship.action.ACTION_DELETE"
        private const val UNKNOWN = "Unknown"

        const val CAN_STORE_REMOTE_LOGS: Boolean = true

        private lateinit var appInstance: FlagshipApplication
        var isInForeground: Boolean = false

        @JvmStatic
        fun getInstance(): FlagshipApplication = appInstance

        private fun getCacheSize(): Int {
            val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            return maxMemory / 8
        }

        @JvmStatic
        fun getVersionName(context: Context): String {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo.versionName ?: UNKNOWN
            } catch (e: PackageManager.NameNotFoundException) {
                return UNKNOWN
            }
        }

        @JvmStatic
        fun getVersionCode(context: Context): Int {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo.versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                return -1
            }
        }
    }

    override fun getVolleyConnector(): VolleyConnector = AppVolleyConnector()

    fun onUserSignOutComplete() {
        coroutineScopeProvider.sync.launch {
            habitTilesRepo.clearCache()
            purchasedArticleManager.clearCache()
        }
    }

    fun onPrivacyConsentStateChanged(functionalityConsent: Boolean?, targetingConsent: Boolean?) {
        functionalityConsent ?: return
        targetingConsent ?: return
        iterableSdk.onPrivacyConsentStateChanged(functionalityConsent && targetingConsent)
    }
}

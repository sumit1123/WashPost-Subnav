package com.wapo.adsinf

import android.app.Application
import android.content.Context
import com.adsbynimbus.Nimbus
import com.amazon.device.ads.AdRegistration
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.wapo.adsinf.interfaces.IAdProvider
import com.wapo.adsinf.interfaces.AdLoadCallback
import com.wapo.adsinf.interfaces.AdRequestFactory
import com.wapo.adsinf.interfaces.AdRequestFactoryResolver
import com.wapo.adsinf.interfaces.DefaultAdRequestFactoryResolver
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdResult
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.utils.AdLoggingUtils
import com.wapo.adsinf.utils.AdLoggingUtils.toDebugString
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.coroutines.resume

/**
 * Orchestrates banner ad loading and provider fallback logic.
 * Initializes the SDKs used for banner ads (APS, Nimbus)
 */
class AdManager private constructor(
    val adProvider: IAdProvider,
    val adRequestFactoryResolver: AdRequestFactoryResolver,
) {

    /**
     * Entry point for loading a banner ad.
     * Starts the fallback chain using providers defined in [AdConfig.adLoadConfig], and displays
     * an offline ad if all providers fail.
     */
    suspend fun loadBannerAd(adView: BannerAdView, adConfig: AdConfig) {
        try {
            val adLoadSession = AdLoadSession(adLoadChain = adConfig.safeAdLoadChain)
            val result = loadBannerAdWithFallbacks(
                adView,
                adConfig,
                adLoadSession,
            )
            Logger.d(TAG, "loadBannerAd: result=$result, loadSession=$adLoadSession")
        } catch (e: Exception) {
            val adError = (e as? AdError) ?: AdError(
                type = AdError.ErrorType.UNKNOWN,
                message = "${e.javaClass.simpleName}: ${e.message}"
            )
            Logger.e(TAG, "loadBannerAd: error=$adError", e)
            AdLoggingUtils.remoteLogFailure(adError)
            adView.showOfflineAd(adConfig)
        }
    }

    /**
     * Iterates through configured ad providers using recursion.
     * Attempts to load an ad using the provider at [adLoaderConfigIndex]. On failure, calls itself
     * with the next index until a provider succeeds or all fallbacks are exhausted.
     */
    private suspend fun loadBannerAdWithFallbacks(
        adView: BannerAdView,
        adConfig: AdConfig,
        adLoadSession: AdLoadSession,
        adLoaderConfigIndex: Int = 0,
    ): AdResult<Unit> {
        val loadChain = adConfig.safeAdLoadChain
        if (adLoaderConfigIndex !in loadChain.indices) {
            adView.showOfflineAd(adConfig)
            return AdResult.Failure(
                error = AdError(
                    type = AdError.ErrorType.INVALID_RENDERER,
                    message = "loadAdWithNetworkFallbacks: Invalid index $adLoaderConfigIndex - adLoadConfigs=${loadChain.joinToString()}",
                ),
            )
        }
        val adLoaderConfig = loadChain[adLoaderConfigIndex]
        val loadAdResponse = loadBannerAd(adView, adConfig, adLoaderConfig, adLoadSession)
        if (loadAdResponse is AdResult.Failure) {
            if (adLoaderConfigIndex < loadChain.size - 1) {
                Logger.d(TAG, "Failed to load ad with $adLoaderConfig. Falling back to ${loadChain.getOrNull(adLoaderConfigIndex + 1)}")
                return loadBannerAdWithFallbacks(
                    adView,
                    adConfig,
                    adLoadSession,
                    adLoaderConfigIndex + 1
                )
            } else {
                Logger.e(TAG, "All adLoaderConfigs failed to load ad. Available adLoadConfigs=${loadChain.joinToString()}")
                adView.showOfflineAd(adConfig)
                return loadAdResponse
            }
        }
        return loadAdResponse
    }

    /**
     * Loads an ad using a specific provider configuration [adLoaderConfig]
     * Creates an AdRequest from the provided configuration and:
     * - on success -> delegates rendering to AdView
     * - on failure -> signals the fallback chain to continue (by returning an AdResult.Failure)
     * This method handles a single provider attempt only
     */
    private suspend fun loadBannerAd(
        adView: BannerAdView,
        adConfig: AdConfig,
        adLoaderConfig: AdLoaderConfig,
        adLoadSession: AdLoadSession,
    ): AdResult<Unit> {
        Logger.d(TAG, "Preparing to load Ad: adLoaderConfig=$adLoaderConfig, adConfig=$adConfig")
        val requestFactory: AdRequestFactory = adRequestFactoryResolver
            .getAdBannerRequestFactory(adLoaderConfig.loaderType)
        adLoadSession.createAttempt()
        val requestResult = requestFactory.createRequest(adConfig, adLoaderConfig, adLoadSession)
        return when (requestResult) {
            is AdResult.Success -> {
                Logger.d(TAG, "AdRequest successfully created: loaderConfig=$adLoaderConfig, adRequest=${requestResult.value.toDebugString()}")
                suspendCancellableCoroutine { continuation ->
                    adView.loadAd(
                        adRequest = requestResult.value,
                        adLoadSession = adLoadSession,
                        callback = object : AdLoadCallback {
                            override fun onAdLoaded() {
                                Logger.d(TAG, "Ad successfully loaded: loaderConfig=$adLoaderConfig")
                                if (continuation.isActive) {
                                    continuation.resume(AdResult.Success(Unit))
                                }
                            }

                            override fun onAdFailed(error: AdError) {
                                Logger.e(TAG, "Ad failed to load: ${error.toDebugString()}")
                                if (continuation.isActive) {
                                    continuation.resume(AdResult.Failure(error))
                                }
                            }
                        }
                    )
                }
            }

            is AdResult.Failure -> {
                Logger.e(TAG, "Error creating AdRequest: ${requestResult.error.toDebugString()}")
                requestResult
            }
        }
    }

    fun onCCPAAdsTrackingUpdated() {
        Nimbus.usPrivacyString = adProvider.ccpaAdsPrivacyString
    }

    fun onConfigUpdated() {
        Nimbus.testMode = isNimbusTestModeEnabled
        if (!isNimbusInitialized && isNimbusEnabled) initNimbusSDK(adProvider.applicationContext)
    }

    companion object {
        private const val TAG = "AdManager"

        @Volatile
        private var instance: AdManager? = null
        private lateinit var adProvider: IAdProvider
        var isNimbusInitialized: Boolean = false
            private set
        var isA9Initialized: Boolean = false
        private val bannersConfig get() = ConfigManager.getInstance().config.adsConfig.bannersConfig
        private val isNimbusEnabled get() = bannersConfig.nimbus.enabled
        private val isNimbusTestModeEnabled get() = adProvider.isDebugBuild && bannersConfig.nimbus.testMode
        private val scope by lazy { MainScope() }

        fun init(adProvider: IAdProvider) {
            this.adProvider = adProvider
            initNimbusSDK(adProvider.applicationContext)
            logBannersConfig()
            scope.launch { logAdIdClientDetails(adProvider.applicationContext) }
        }

        private fun initNimbusSDK(context: Context) {
            if (!isNimbusEnabled) return
            try {
                val (publisherKey, apiKey) = when {
                    adProvider.isDebugBuild -> WapoSecDataProvider.nimbusPublisherKeyDev to WapoSecDataProvider.nimbusApiKeyDev
                    else -> WapoSecDataProvider.nimbusPublisherKeyProd to WapoSecDataProvider.nimbusApiKeyProd
                }
                Nimbus.initialize(context, publisherKey, apiKey)
                Nimbus.testMode = isNimbusTestModeEnabled
                Nimbus.usPrivacyString = adProvider.ccpaAdsPrivacyString
                Nimbus.COPPA = false
                if (adProvider.isDebugBuild) {
                    Nimbus.addLogger { level, message ->
                        Logger.log(level, "NimbusAds", message)
                    }
                }
                isNimbusInitialized = true
            } catch (e: Exception) {
                Logger.e(TAG, "Error initializing Nimbus ${e.message}", e)
                RemoteLog.e(
                    context, EventLog.Builder()
                        .setModule(LogModules.ADS)
                        .setMessage("Error initializing Nimbus")
                        .setErrorMessage(e.message)
                        .build()
                )
            }
        }

        fun initializeA9Ads(app: Application) {
            AdRegistration.getInstance(bannersConfig.dtb.app, app)
            if (adProvider.isDebugBuild) {
                AdRegistration.enableLogging(true)
                AdRegistration.enableTesting(true)
            }
        }

        private fun logBannersConfig() {
            if (adProvider.isDebugBuild) {
                scope.launch(Dispatchers.Default) {
                    ConfigManager.getInstance().state
                        .filterNotNull()
                        .distinctUntilChangedBy { it.config.adsConfig.bannersConfig }
                        .collect {
                            Logger.d(TAG, "bannersConfig: $bannersConfig")
                        }
                }
            }
        }

        /**
         * Approximately 50% of users are classified as anonymous within Google Ad Manager (GAM)
         * because their ad requests lack an ad_id. This method has been implemented to debug and verify
         * whether Limit Ad Tracking (LAT) is enabled on those specific devices.
         * Implemented as a single call per session.
         */
        private suspend fun logAdIdClientDetails(applicationContext: Context) =
            withContext(Dispatchers.IO) {
                val eventLogBuilder = EventLog.Builder()
                    .setMessage("Ad Client Details")
                    .setModule(LogModules.ADS)
                try {
                    // Calling this API from main thread can lead to deadlock
                    val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
                    val adId = adInfo.id
                    // Check if the ID itself is null, empty, or the "zero string"
                    val isZeroed = adId.isNullOrEmpty() ||
                            adId == "00000000-0000-0000-0000-000000000000"
                    eventLogBuilder.set("isAdIdZeroed", isZeroed)
                    eventLogBuilder.set("lat", adInfo.isLimitAdTrackingEnabled)
                } catch (e: kotlin.Exception) {
                    eventLogBuilder.setErrorMessage(e.message)
                }
                RemoteLog.d(applicationContext, eventLogBuilder.build())
            }

        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    AdManager(adProvider, DefaultAdRequestFactoryResolver()).also {
                        instance = it
                    }
                }
            }
        }
    }
}

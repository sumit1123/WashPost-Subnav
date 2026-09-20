package com.wapo.flagship.features.audio.ads.repository

import android.content.Context
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.PlayerType.AUTOMATED
import com.wapo.flagship.features.audio.PlayerType.HUMAN
import com.wapo.flagship.features.audio.PlayerType.PODCAST
import com.wapo.flagship.features.audio.PlayerType.STANDALONE
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.getConfig
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.toAdBreak
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.toAudioAdConfig
import com.wapo.flagship.features.audio.ads.model.AdRequestContext
import com.wapo.flagship.features.audio.ads.model.AudioAdBreak
import com.wapo.flagship.features.audio.ads.model.AudioAdBreakType
import com.wapo.flagship.features.audio.ads.model.AudioAdConfig
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioAdsRepositoryImpl @Inject constructor(
    private val configManager: ConfigManager,
    private val audioProvider: AudioProvider,
    private val imaAdTagBuilder: ImaAdTagBuilder,
    @ApplicationContext private val appContext: Context,
) : AudioAdsRepository {
    override fun getAdsConfig(audioMediaConfig: AudioMediaConfig): AudioAdConfig? {
        val playerType = audioMediaConfig.getPlayerType()
        val appConfig = configManager.config.adsConfig.audio.getConfig(playerType)
        if (
            appConfig?.vastEnabled != true ||
            audioProvider.shouldSuppressAds() ||
            playerType !in setOf(PODCAST, AUTOMATED, HUMAN, STANDALONE)
        ) return null

        val adConfig = getAudioAdConfig(audioMediaConfig, appConfig)
        if (adConfig == null || adConfig.adSetUrl.isEmpty() || adConfig.adBreaks.isNullOrEmpty()) {
            Logger.d(TAG, "getAdsConfig => null (missing adConfig, adSetUrl or empty adBreaks)")
            return null
        }
        val adRequestContext = getAdRequestContext(appConfig)

        val adBreaks = adConfig.adBreaks
            .mapIndexed { index, adBreak ->
                val adBreakType = when (adBreak) {
                    is AudioMediaAdBreak.Preroll -> AudioAdBreakType.PREROLL
                    is AudioMediaAdBreak.Midroll -> AudioAdBreakType.MIDROLL
                    is AudioMediaAdBreak.Postroll -> AudioAdBreakType.POSTROLL
                }
                val adTagUrl = imaAdTagBuilder.buildAudioAdTagUrl(
                    audioMediaAdConfig = adConfig,
                    adBreak = adBreak,
                    adBreakIndex = index + 1,
                    adRequestContext = adRequestContext,
                )
                val timeMs = when (adBreak) {
                    is AudioMediaAdBreak.Midroll -> adBreak.timeMs
                    else -> null
                }
                AudioAdBreak(adBreakType, adTagUrl, timeMs)
            }
        return AudioAdConfig(contentType = playerType, adBreaks = adBreaks)
            .also { Logger.d(TAG, "getAdsConfig => $it") }
    }

    private fun getAudioAdConfig(
        audioMediaConfig: AudioMediaConfig,
        appConfig: com.washingtonpost.android.config.domain.models.AudioAdConfig,
    ): AudioMediaAdConfig? {
        val playerType = audioMediaConfig.getPlayerType()
        val adConfig = audioMediaConfig.adConfig
            ?: appConfig.toAudioAdConfig()
            ?: return null

        if (adConfig.adSetUrl.isEmpty()) {
            Logger.e(TAG, "AdSetUrl cannot be empty")
            return null
        }

        // Complete JsonApp ad config with local config.json fallbacks.
        val adBreaks = adConfig.adBreaks ?: appConfig.adBreaks?.mapNotNull { it.toAdBreak() }
        val primarySectionId = adConfig.primarySectionId ?: appConfig.macros.primarySectionId
        val seriesName = adConfig.seriesName
            ?: if (playerType == PODCAST) audioMediaConfig.primaryLabel else null
        val contentLanguage = adConfig.contentLanguage ?: appConfig.macros.contentLanguage
        val tritonExtStid = when {
            !adConfig.tritonExtStid.isNullOrEmpty() -> adConfig.tritonExtStid
            playerType == PODCAST -> when {
                !audioMediaConfig.series.isNullOrEmpty() -> "wp-${audioMediaConfig.series}"
                else -> appConfig.macros.tritonExtStid
            }
            else -> appConfig.macros.tritonExtStid
        }
        val tritonFeedType = adConfig.tritonFeedType ?: appConfig.macros.tritonFeedType
        val tritonDeliveryMethod = adConfig.tritonDeliveryMethod ?: appConfig.macros.tritonDeliveryMethod

        return adConfig.copy(
            adBreaks = adBreaks,
            primarySectionId = primarySectionId,
            seriesName = seriesName,
            contentLanguage = contentLanguage,
            tritonExtStid = tritonExtStid,
            tritonFeedType = tritonFeedType,
            tritonDeliveryMethod = tritonDeliveryMethod,
        )
    }

    private fun getAdRequestContext(
        appConfig: com.washingtonpost.android.config.domain.models.AudioAdConfig,
    ): AdRequestContext {
        return AdRequestContext(
            appIdentity = AdRequestContext.AppIdentity(
                bundleId = appContext.packageName,
                storeId = appConfig.macros.storeId,
                storeUrl = appConfig.macros.storeUrl,
                siteUrl = appConfig.macros.siteUrl,
            ),
            userPrivacyConsent = audioProvider.getUserPrivacyConsentForAds(appContext),
        )
    }

    companion object {
        private const val TAG = "AudioAdsRepositoryImpl"
    }
}
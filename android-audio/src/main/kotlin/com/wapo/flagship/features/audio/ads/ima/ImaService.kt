package com.wapo.flagship.features.audio.ads.ima

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isAd
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import com.wapo.flagship.features.audio.service2.media.extensions.isAd
import com.wapo.flagship.features.audio.utils.PlayerLogger
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class ImaService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configManager: ConfigManager,
    private val playerLogger: PlayerLogger,
    private val musicServiceConnection: MusicServiceConnection,
) : AdErrorEvent.AdErrorListener, AdEvent.AdEventListener, AdsLoader.AdsLoadedListener,
    Player.Listener {
    private val audioAdsConfig get() = configManager.config.adsConfig.audio
    private val sdkFactory: ImaSdkFactory = ImaSdkFactory.getInstance()
    private var imaAdsPlayer: ImaAdsPlayer? = null
    private var adsLoader: AdsLoader? = null
    private var adsManager: AdsManager? = null
    private val _isPlayingAd = MutableStateFlow(false)
    val isPlayingAd = _isPlayingAd.asStateFlow()
    var adWasSkipped: Boolean = false
        private set

    fun attachPlayer(player: Player) {
        if (!audioAdsConfig.configsByContentType.any { it.value.vastEnabled }) return
        when {
            imaAdsPlayer?.basePlayer == player -> {
                Logger.w(TAG, "Player already added")
                return
            }
            imaAdsPlayer?.basePlayer != null -> {
                Logger.w(TAG, "A player had already been added. Releasing current ImaService")
                release()
            }
        }
        imaAdsPlayer = ImaAdsPlayer(player, musicServiceConnection, context.resources, playerLogger).apply {
            addListener(this@ImaService)
        }
        val adDisplayContainer =
            ImaSdkFactory.createAudioAdDisplayContainer(context, imaAdsPlayer!!)
        adsLoader = sdkFactory.createAdsLoader(context, imaSdkSettings, adDisplayContainer).apply {
            addAdErrorListener(this@ImaService)
            addAdsLoadedListener(this@ImaService)
        }
    }

    fun playAd(adTagUrl: String) {
        Logger.d(TAG, "playAd: adTagUrl=$adTagUrl")
        if (imaAdsPlayer == null || adsLoader == null) {
            Logger.e(TAG, "ImaAdsController can't play an ad before attaching a player")
            return
        }

        imaAdsPlayer?.pauseContent()
        val request = sdkFactory.createAdsRequest().apply {
            this.adTagUrl = adTagUrl
            imaAdsPlayer?.let {
                contentProgressProvider = ContentProgressProvider {
                    VideoProgressUpdate(it.currentPosition, it.duration)
                }
            }
        }
        adsLoader?.requestAds(request)
    }

    fun skipAd() {
        Logger.d(TAG, "skipAd")
        adsManager?.skip()
        imaAdsPlayer?.skipAd()
        adWasSkipped = true
    }

    //  region AdsLoader.AdsLoadedListener callbacks

    override fun onAdsManagerLoaded(adsManagerLoadedEvent: AdsManagerLoadedEvent) {
        Logger.i(TAG, "onAdsManagerLoaded: event=$adsManagerLoadedEvent")
        adsManager = adsManagerLoadedEvent.adsManager
            .apply {
                addAdErrorListener(this@ImaService)
                addAdEventListener(this@ImaService)
                init()
            }
    }

    //  endregion

    //  region AdErrorEvent.AdErrorListener callbacks

    override fun onAdError(adErrorEvent: AdErrorEvent) {
        Logger.e(TAG, "Ad Error: ${adErrorEvent.error.message}")
        adWasSkipped = true
        imaAdsPlayer?.resumeContent()
    }

    //  endregion

    //  region AdEvent.AdEventListener

    @UnstableApi
    override fun onAdEvent(adEvent: AdEvent) {
        Logger.i(TAG, "Event: ${adEvent.type}")
        when (adEvent.type) {
            AdEvent.AdEventType.LOADED -> {
                adWasSkipped = false
                adsManager?.start()
            }
            AdEvent.AdEventType.CONTENT_PAUSE_REQUESTED -> imaAdsPlayer?.pauseContent()
            AdEvent.AdEventType.CONTENT_RESUME_REQUESTED -> imaAdsPlayer?.resumeContent()
            AdEvent.AdEventType.ALL_ADS_COMPLETED -> {
                adsManager?.destroy()
                adsManager = null
            }

            else -> {}
        }
    }

    //  endregion

    //  region Player.Listener callbacks

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Logger.d(TAG, "onMediaItemTransition: mediaItem=$mediaItem, isPlayingAd=${mediaItem?.isAd}")
        _isPlayingAd.value = mediaItem?.isAd == true
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        Logger.d(TAG, "onMediaMetadataChanged: mediaMetadata=$mediaMetadata, isPlayingAd=${mediaMetadata.isAd == true}")
        _isPlayingAd.value = mediaMetadata.isAd == true
    }

    //  endregion

    fun release() {
        Logger.d(TAG, "release")
        imaAdsPlayer?.removeListener(this)
        imaAdsPlayer?.releaseAll()
        imaAdsPlayer = null
        adsLoader?.removeAdsLoadedListener(this)
        adsLoader?.removeAdErrorListener(this)
        adsLoader?.release()
        adsLoader = null
    }

    companion object {
        private const val TAG = "ImaService"

        fun initializeIMASDK(context: Context) {
            ImaSdkFactory.getInstance().initialize(context, imaSdkSettings)
        }

        private val imaSdkSettings: ImaSdkSettings
            get() = ImaSdkFactory.getInstance().createImaSdkSettings()
    }
}
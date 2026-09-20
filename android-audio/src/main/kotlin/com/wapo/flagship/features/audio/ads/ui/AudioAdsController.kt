package com.wapo.flagship.features.audio.ads.ui

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.AudioAdsFlag
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.ads.model.AudioAdBreak
import com.wapo.flagship.features.audio.ads.ima.ImaService
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.getConfig
import com.wapo.flagship.features.audio.ads.model.AudioAdBreakType
import com.wapo.flagship.features.audio.ads.model.AudioAdConfig
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.adsConfig
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isPlayingAd2
import com.wapo.flagship.features.audio.utils.PlayerLogger
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class AudioAdsController @Inject constructor(
    private val imaService: ImaService,
    private val configManager: ConfigManager,
    private val logger: PlayerLogger,
) : Player.Listener {
    private val audioAdsConfig get() = configManager.config.adsConfig.audio
    private var player: Player? = null
    val isPlayingAd = imaService.isPlayingAd

    private val playedAdBreaksLock = Any()
    private val playedAdBreaks = mutableListOf<AudioAdBreak>()
    private var currentContentMediaItemId: String? = null

    private val scope = MainScope()
    private var monitoringJob: Job? = null

    fun attachPlayer(player: Player) {
        if (!audioAdsConfig.configsByContentType.any { it.value.vastEnabled }) return
        when {
            this.player == player -> {
                Logger.w(TAG, "Player already added")
                return
            }

            this.player != null -> {
                Logger.w(TAG, "A player had already been added. Releasing current AudioAdsController")
                release()
            }
        }
        imaService.attachPlayer(player)
        this.player = player
        this.player?.addListener(this)
    }

    fun playAdBreak(adBreak: AudioAdBreak, contentType: PlayerType) {
        Logger.d(TAG, "playAdBreak: contentType=$contentType, adBreak=$adBreak")
        val adConfig = audioAdsConfig.getConfig(contentType) ?: return
        if (!adConfig.vastEnabled) return
        val shouldPlayAd = AudioAdsFlag.shouldPlayAd(
            globalIntervalMs = adConfig.minGlobalAdIntervalSeconds?.times(1000)?.toLong(),
            adBreakIntervalMs = adConfig.minAdBreakIntervalSeconds?.times(1000)?.toLong(),
        )
        if (!shouldPlayAd) {
            addPlayedAdBreak(adBreak)
            return
        }

        imaService.playAd(adBreak.adTagUrl)
        addPlayedAdBreak(adBreak)
    }

    fun skipAd() {
        imaService.skipAd()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Logger.d(TAG, "onMediaItemTransition: mediaItem=${logger.mediaItemDetailsToString(mediaItem)}, reason=${logger.mediaItemTransitionReasonToString(reason)}, player=${logger.playerDetailsToString(player)}")
        val player = player ?: return
        val isReturningFromAd = currentContentMediaItemId == mediaItem?.mediaId
        if (mediaItem == null || player.isPlayingAd2 || isReturningFromAd) {
            player.playWhenReady = true
            if (mediaItem == null) currentContentMediaItemId = null
            if (isReturningFromAd && !imaService.adWasSkipped) AudioAdsFlag.updateTimestampsOnAdCompleted()
            return
        }

        clearPlayedAdBreaks()
        AudioAdsFlag.resetLastAdBreakTimestamp()
        currentContentMediaItemId = mediaItem.mediaId

        val adsConfig = mediaItem.adsConfig ?: return
        maybePlayPreroll(player.currentPosition, adsConfig)
        startMonitoringJob(player, adsConfig)
    }

    private fun startMonitoringJob(player: Player, adsConfig: AudioAdConfig) {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                delay(MONITORING_UPDATES_MS)

                if (player.isPlayingAd2) continue
                maybePlayMidroll(player.currentPosition, adsConfig)
                maybePlayPostroll(player, adsConfig).also { playedAd ->
                    if (playedAd) monitoringJob?.cancel()
                }
            }
        }
    }

    private fun maybePlayPreroll(currentPositionMs: Long, adsConfig: AudioAdConfig): Boolean {
        Logger.d(TAG, "maybePlayPreroll ${logger.playerDetailsToString(player)}")
        if (currentPositionMs < MONITORING_UPDATES_MS) {
            val preroll = adsConfig.adBreaks.firstOrNull {
                it.type == AudioAdBreakType.PREROLL && !hasAdBreakPlayed(it)
            }
            preroll?.let {
                playAdBreak(it, adsConfig.contentType)
                return true
            }
        }
        return false
    }

    private fun maybePlayMidroll(currentPositionMs: Long, adsConfig: AudioAdConfig): Boolean {
        Logger.v(TAG, "maybePlayMidroll ${logger.playerDetailsToString(player)}")
        val midroll = adsConfig.adBreaks.firstOrNull {
            it.type == AudioAdBreakType.MIDROLL && !hasAdBreakPlayed(it) &&
            it.timeMs != null && currentPositionMs >= it.timeMs
        }
        midroll?.let {
            playAdBreak(it, adsConfig.contentType)
            return true
        }
        return false
    }

    private fun maybePlayPostroll(player: Player, adsConfig: AudioAdConfig): Boolean {
        Logger.v(TAG, "maybePlayPostroll: player=${logger.playerDetailsToString(player)}")
        if (player.duration > 0 && player.currentPosition in player.duration - MONITORING_UPDATES_MS..player.duration) {
            val postroll = adsConfig.adBreaks.firstOrNull {
                it.type == AudioAdBreakType.POSTROLL && !hasAdBreakPlayed(it)
            }
            postroll?.let {
                playAdBreak(it, adsConfig.contentType)
                return true
            }
        }
        return false
    }

    private fun addPlayedAdBreak(adBreak: AudioAdBreak) = synchronized(playedAdBreaksLock) {
        playedAdBreaks.add(adBreak)
    }

    private fun clearPlayedAdBreaks() = synchronized(playedAdBreaksLock) {
        playedAdBreaks.clear()
    }

    private fun hasAdBreakPlayed(adBreak: AudioAdBreak): Boolean =
        synchronized(playedAdBreaksLock) {
            playedAdBreaks.contains(adBreak)
        }

    fun release() {
        Logger.d(TAG, "release")
        monitoringJob?.cancel()
        monitoringJob = null
        player?.removeListener(this)
        player = null
    }

    companion object {
        private const val TAG = "AudioAdsController"
        private const val MONITORING_UPDATES_MS: Long = 500
    }
}
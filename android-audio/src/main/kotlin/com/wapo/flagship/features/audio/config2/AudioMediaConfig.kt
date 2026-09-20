/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.config2

import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.AudioAdsFlag
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.isVastEnabled
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.washingtonpost.android.config.domain.manager.ConfigManager
import java.util.UUID

/**
 * App can construct either [AudioMediaConfigList] or [AudioMediaConfig] objects and call
 * [com.wapo.flagship.features.audio.ClassicAudioManager2] play methods to play any supported
 * audio item. [AudioMediaConfig] will find a config `id` and player type based on the data provided
 * to the config. Data should be resolved and should be a known [PlayerType] to be played
 * by the audio library.
 */
data class AudioMediaConfigList(
    val id: String? = null,
    val list: MutableList<AudioMediaConfig>
)

data class AudioMediaConfig(
    private val playerType: PlayerType? = null,
    val mediaId: String? = null,
    val humanAdsUrl: String? = null,
    val humanRawUrl: String? = null,
    val manifestUrl: String? = null,
    val adsUrl: String? = null,
    val rawUrl: String? = null,
    val title: String? = null,
    val titlePrefix: String? = null,
    val titleSeparator: String? = null,
    val subtitle: String? = null,
    val date: Long? = null,
    val imageUrl: String? = null,
    val imageCaption: String? = null,
    val duration: Long? = null,
    val streamUrl: String? = null,
    val streamUrlNoAds: String? = null,
    val contentUrl: String? = null,
    val sectionName: String? = null,
    val caption: String? = null,
    val labelType: String? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    var voices: MutableList<PlaybackVoice>? = null,
    val arcId: String? = null,
    val audioTracking: AudioTracker? = null,
    val adsCustomTargeting: Map<String, List<String>>? = null,
    val labelStyle: String? = null,
    var positionToStartInMs: Long? = null,
    val url: String? = null,
    var children: List<AudioMediaConfig>? = null,
    var transitions: List<AudioMediaConfig>? = null,
    val type: String? = null,
    val audioType: String? = null,
    val pinned: Boolean? = false,
    val series: String? = null,
    val adConfig: AudioMediaAdConfig? = null,
    val isShared: Boolean? = false,
    val seriesSlug: String? = null,
    val podcastSlug: String? = null,
    val subscriptionLinks: AudioMediaSubscriptionLinks? = null,
) {
    private val tag = "AudioMediaConfig"
    private val audioAdsConfig get() = ConfigManager.getInstance().config.adsConfig.audio
    private var resolvedPlayerType: PlayerType
    private var resolvedPlayAd: Boolean? = null
    var resolvedMediaUrl: String? = null
    var resolvedAdsUrl: String? = null
    var id: String
        private set
    var isActionAudio: Boolean = false
        set(value) {
            field = value
            audioTracking?.updateAudioFromActionBarFlag(value)
        }

    var isAudioCarousel: Boolean = false
        set(value) {
            field = value
            audioTracking?.updateIsCarousal(value)
        }

    init {
        resolvedPlayerType = resolvePlayerType()
        id = resolveId()
    }

    fun resolveAdState(adInterval: Long, shouldSuppressAds: Boolean) {
        val isVastEnabled = audioAdsConfig.isVastEnabled(getPlayerType(), this)
        resolvedPlayAd = AudioAdsFlag.shouldPlayAd(adInterval, updateTimestamp = !isVastEnabled) && !shouldSuppressAds
        audioTracking?.updatePlayAd(if (resolvedPlayAd == true) "T" else "F")
    }

    fun getPlayerType(): PlayerType {
        return playerType ?: resolvedPlayerType
    }

    fun getPlayAd(): Boolean {
        return resolvedPlayAd ?: false
    }

    private fun resolvePlayerType(): PlayerType {
        return when {
            mediaId != null -> PlayerType.PODCAST
            humanRawUrl != null || (!audioAdsConfig.isVastEnabled(PlayerType.HUMAN, this) && humanAdsUrl != null) -> PlayerType.HUMAN
            manifestUrl != null || rawUrl != null || !voices.isNullOrEmpty() || url != null || (!audioAdsConfig.isVastEnabled(PlayerType.AUTOMATED, this) && adsUrl != null) -> PlayerType.AUTOMATED
            else -> playerType ?: PlayerType.UNKNOWN
        }
    }

    private fun resolveId(): String {
        // arcId is not working as expected. Main idea is to identify the media item wherever it is
        // in the app.
        //if (!arcId.isNullOrEmpty()) return arcId
        val isVastEnabled = audioAdsConfig.isVastEnabled(playerType ?: resolvedPlayerType, this)
        return when (playerType ?: resolvedPlayerType) {
            PlayerType.PODCAST, PlayerType.PERSO_PODCAST -> {
                if (!mediaId.isNullOrEmpty()) {
                    mediaId
                } else {
                    Logger.e(
                        tag,
                        "mediaId should not be null or empty for a PlayerType.PODCAST type"
                    )
                    generateRandomId()
                }
            }
            PlayerType.AUTOMATED -> {
                val id = if (!manifestUrl.isNullOrEmpty())
                    manifestUrl
                else if (!isVastEnabled && !adsUrl.isNullOrEmpty() && resolvedPlayAd == true)
                    adsUrl
                else if (!rawUrl.isNullOrEmpty())
                    rawUrl
                else if(!url.isNullOrEmpty())
                    url
                else null

                val voiceId = if (!isVastEnabled && !voices.isNullOrEmpty() && !voices!![0].adsUrl.isNullOrEmpty() && resolvedPlayAd == true)
                    voices!![0].adsUrl
                else if (!voices.isNullOrEmpty() && !voices!![0].rawUrl.isNullOrEmpty())
                    voices!![0].rawUrl
                else null

                if (id != null) {
                    id
                } else if (voiceId != null) {
                    voiceId
                } else {
                    Logger.e(
                        tag,
                        "One of the urls(manifestUrl, adsUrl, rawUrl, voices.adsUrl, voices.rawUrl) should not be null or empty for a PlayerType.AUTOMATED config"
                    )
                    generateRandomId()
                }
            }
            PlayerType.HUMAN -> {
                if (!isVastEnabled && !humanAdsUrl.isNullOrEmpty() && resolvedPlayAd == true)
                    humanAdsUrl
                else if (!humanRawUrl.isNullOrEmpty())
                    humanRawUrl
                else {
                    Logger.e(
                        tag,
                        "Either humanAdsUrl or humanRawUrl should not be null for a PlayerType.HUMAN config"
                    )
                    generateRandomId()
                }
            }
            PlayerType.STANDALONE -> {
                if (!rawUrl.isNullOrEmpty())
                    rawUrl
                else {
                    Logger.e(
                        tag,
                        "rawUrl should not be null or empty for a PlayerType.STANDALONE config"
                    )
                    generateRandomId()
                }
            }
            PlayerType.UNKNOWN -> {
                generateRandomId()
            }
        }
    }

    private fun generateRandomId(): String {
        return UUID.randomUUID().toString()
    }
}

fun getStyle(style: String?): LabelStyle? {
    return when (style) {
        "opinions" -> LabelStyle.OPINIONS
        else -> null
    }
}

enum class LabelStyle {
    OPINIONS
}

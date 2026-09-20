package com.wapo.flagship.features.audio.ads.mappers

import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.PlayerType.AUTOMATED
import com.wapo.flagship.features.audio.PlayerType.HUMAN
import com.wapo.flagship.features.audio.PlayerType.PODCAST
import com.wapo.flagship.features.audio.PlayerType.STANDALONE
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.washingtonpost.android.config.domain.models.AudioAdConfig
import com.washingtonpost.android.config.domain.models.AudioAdsConfig
import com.washingtonpost.android.config.domain.models.AudioContentType

object AudioAdAppConfigMapper {
    fun PlayerType.toAudioContentType(): AudioContentType? = when (this) {
        PODCAST -> AudioContentType.PODCAST
        AUTOMATED, HUMAN, STANDALONE -> AudioContentType.ARTICLE
        else -> null
    }

    fun AudioAdsConfig.getConfig(playerType: PlayerType): AudioAdConfig? {
        return configsByContentType[playerType.toAudioContentType()]
    }

    fun AudioAdsConfig.isVastEnabled(playerType: PlayerType, audioMediaConfig: AudioMediaConfig?): Boolean {
        val configVastEnabled = getConfig(playerType)?.vastEnabled ?: false
        val mediaUseVAST = audioMediaConfig?.adConfig?.useVAST ?: true
        return configVastEnabled && mediaUseVAST
    }

    fun AudioAdsConfig.isVastEnabled(audioMediaConfig: AudioMediaConfig): Boolean {
        return isVastEnabled(audioMediaConfig.getPlayerType(), audioMediaConfig)
    }

    fun AudioAdConfig.toAudioAdConfig(): AudioMediaAdConfig? {
        if (vastTemplate.isBlank()) return null
        return AudioMediaAdConfig(
            adSetUrl = vastTemplate,
            adBreaks = adBreaks?.mapNotNull { it.toAdBreak() },
            primarySectionId = macros.primarySectionId,
        )
    }

    fun AudioAdConfig.AdBreak.toAdBreak(timeSeconds: Int? = null): AudioMediaAdBreak? {
        return when (type) {
            "preroll" -> AudioMediaAdBreak.Preroll(maxAds)
            "midroll" -> {
                val timeMs = (this.timeSeconds ?: timeSeconds)?.times(1000)?.toLong()
                if (timeMs == null || timeMs < 0) return null
                AudioMediaAdBreak.Midroll(maxAds, timeMs)
            }
            "postroll" -> AudioMediaAdBreak.Postroll(maxAds)
            else -> null
        }
    }
}

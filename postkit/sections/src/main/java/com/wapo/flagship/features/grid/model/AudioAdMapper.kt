package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.grid.AudioAdBreakEntity
import com.wapo.flagship.features.grid.AudioAdBreakType
import com.wapo.flagship.features.grid.AudioAdConfigEntity

object AudioAdMapper {
    fun getAudioAdConfig(audioAdConfigEntity: AudioAdConfigEntity?): AudioMediaAdConfig? {
        audioAdConfigEntity ?: return null
        return AudioMediaAdConfig(
            adSetUrl = audioAdConfigEntity.adSetUrl.orEmpty(),
            adBreaks = audioAdConfigEntity.adBreaks?.mapNotNull { getAudioAdBreak(it) },
            primarySectionId = audioAdConfigEntity.primarySectionId,
            useVAST = audioAdConfigEntity.useVAST,
        )
    }

    private fun getAudioAdBreak(audioAdBreakEntity: AudioAdBreakEntity?): AudioMediaAdBreak? {
        audioAdBreakEntity ?: return null
        return when (audioAdBreakEntity.type) {
            AudioAdBreakType.PREROLL -> AudioMediaAdBreak.Preroll(
                maxAds = audioAdBreakEntity.maxAds ?: 1
            )

            AudioAdBreakType.MIDROLL -> AudioMediaAdBreak.Midroll(
                maxAds = audioAdBreakEntity.maxAds ?: 2,
                timeMs = audioAdBreakEntity.time?.times(1000)?.toLong() ?: return null,
            )

            AudioAdBreakType.POSTROLL -> AudioMediaAdBreak.Postroll(
                maxAds = audioAdBreakEntity.maxAds ?: 2
            )

            else -> null
        }
    }
}

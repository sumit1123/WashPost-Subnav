package com.wapo.flagship.features.audio.ads.repository

import com.wapo.flagship.features.audio.ads.model.AudioAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig

interface AudioAdsRepository {
    fun getAdsConfig(audioMediaConfig: AudioMediaConfig): AudioAdConfig?
}

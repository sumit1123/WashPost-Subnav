/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid

import com.wapo.flagship.features.grid.model.AudioArticle
import com.wapo.flagship.features.grid.model.HumanVoice
import com.wapo.flagship.features.grid.model.Voice

class VoiceUtils {
    companion object {
        fun getPreferredVoice(audioArticle: AudioArticle?): Any? {
            return audioArticle?.humanVoice
                ?: audioArticle?.voices?.firstOrNull()
        }

        fun getPreferredVoiceUrl(audioArticle: AudioArticle?): String? {
            return when (val preferredVoice = getPreferredVoice(audioArticle)) {
                is HumanVoice -> preferredVoice.rawUrl
                is Voice -> preferredVoice.rawUrl
                else -> null
            }
        }

        fun getPreferredVoiceDuration(audioArticle: AudioArticle?): Long? {
            return when (val preferredVoice = getPreferredVoice(audioArticle)) {
                is HumanVoice -> preferredVoice.duration
                is Voice -> preferredVoice.duration
                else -> null
            }
        }
    }
}
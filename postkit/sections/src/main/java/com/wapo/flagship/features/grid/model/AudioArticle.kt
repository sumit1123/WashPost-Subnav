package com.wapo.flagship.features.grid.model

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.grid.AudioArticleVoiceType

data class AudioArticle(
    val contentUrl: String?,
    val displayDate: String?,
    val label: CompoundLabel?,
    val titlePrefix: String?,
    val titleSeparator: String?,
    val title: String?,
    val voices: List<Voice>?,
    val humanVoice: HumanVoice?,
    val preferredVoiceType: AudioArticleVoiceType?,
    val tracking: AudioArticleTracking?,
    val feed: String?,
    val playerMedia: Media?,
    val inlinePlayer: InlinePlayer?,
    val caption: String?,
    val language: String? = null,
    val adConfig: AudioMediaAdConfig? = null,
) {
    init {
        val message =
            if (preferredVoiceType == AudioArticleVoiceType.AUTOMATED && voices.isNullOrEmpty()) {
                "Manifest voices are empty or null"
            } else if (preferredVoiceType == AudioArticleVoiceType.HUMAN
                && humanVoice?.adsUrl.isNullOrEmpty() && humanVoice?.rawUrl.isNullOrEmpty()
            ) {
                "HumanRead voices are empty or null"
            } else null
        if (message != null) {
            EventLog.Builder().apply {
                setMessage(message)
                setModule(LogModules.SECTIONS)
                set("content_url", contentUrl)
                set("voices_empty", voices?.isEmpty())
                set("human_ads_url", humanVoice?.adsUrl)
                set("human_raw_url", humanVoice?.rawUrl)
            }.run {
                RemoteLog.d(AppContextUtils.appContext, this.build())
            }
        }
    }
}
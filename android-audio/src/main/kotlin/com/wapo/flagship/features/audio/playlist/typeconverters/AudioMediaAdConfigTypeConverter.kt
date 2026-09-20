package com.wapo.flagship.features.audio.playlist.typeconverters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig

class AudioMediaAdConfigTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun toJson(config: AudioMediaAdConfig?): String? = config?.let {
        gson.toJson(
            StoredAudioMediaAdConfig(
                adSetUrl = it.adSetUrl,
                adBreaks = it.adBreaks?.map { adBreak ->
                    when (adBreak) {
                        is AudioMediaAdBreak.Preroll -> StoredAudioMediaAdBreak("preroll", adBreak.maxAds)
                        is AudioMediaAdBreak.Midroll -> StoredAudioMediaAdBreak("midroll", adBreak.maxAds, adBreak.timeMs)
                        is AudioMediaAdBreak.Postroll -> StoredAudioMediaAdBreak("postroll", adBreak.maxAds)
                    }
                },
                primarySectionId = it.primarySectionId,
                useVAST = it.useVAST,
                seriesName = it.seriesName,
                contentLanguage = it.contentLanguage,
                tritonExtStid = it.tritonExtStid,
                tritonFeedType = it.tritonFeedType,
                tritonDeliveryMethod = it.tritonDeliveryMethod,
            )
        )
    }

    @TypeConverter
    fun fromJson(json: String?): AudioMediaAdConfig? {
        val stored = json?.let { gson.fromJson(it, StoredAudioMediaAdConfig::class.java) } ?: return null
        return AudioMediaAdConfig(
            adSetUrl = stored.adSetUrl,
            adBreaks = stored.adBreaks?.mapNotNull { adBreak ->
                when (adBreak.type) {
                    "preroll" -> AudioMediaAdBreak.Preroll(adBreak.maxAds)
                    "midroll" -> adBreak.timeMs?.let { AudioMediaAdBreak.Midroll(adBreak.maxAds, it) }
                    "postroll" -> AudioMediaAdBreak.Postroll(adBreak.maxAds)
                    else -> null
                }
            },
            primarySectionId = stored.primarySectionId,
            useVAST = stored.useVAST,
            seriesName = stored.seriesName,
            contentLanguage = stored.contentLanguage,
            tritonExtStid = stored.tritonExtStid,
            tritonFeedType = stored.tritonFeedType,
            tritonDeliveryMethod = stored.tritonDeliveryMethod,
        )
    }
}

private data class StoredAudioMediaAdConfig(
    @SerializedName("ad_set_url") val adSetUrl: String,
    @SerializedName("ad_breaks") val adBreaks: List<StoredAudioMediaAdBreak>?,
    @SerializedName("primary_section_id") val primarySectionId: String?,
    @SerializedName("use_vast") val useVAST: Boolean? = null,
    @SerializedName("series_name") val seriesName: String?,
    @SerializedName("content_language") val contentLanguage: String?,
    @SerializedName("triton_ext_stid") val tritonExtStid: String?,
    @SerializedName("triton_feed_type") val tritonFeedType: String?,
    @SerializedName("triton_delivery_method") val tritonDeliveryMethod: String?,
)

private data class StoredAudioMediaAdBreak(
    @SerializedName("type") val type: String,
    @SerializedName("max_ads") val maxAds: Int,
    @SerializedName("time_ms") val timeMs: Long? = null,
)

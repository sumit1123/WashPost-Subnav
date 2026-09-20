package com.wapo.flagship.features.audio.podcast

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor.Companion.headers
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.PlayerType.PODCAST
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.getConfig
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.toAdBreak
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.toAudioAdConfig
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config2.AudioMediaAdBreak
import com.wapo.flagship.features.audio.config2.AudioMediaAdConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaSubscriptionLinks
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Completes podcast configurations with Lionfish metadata when the originating response did not
 * provide everything needed for playback and podcast tracking.
 *
 */
@Singleton
class PodcastMetadataResolver @Inject constructor(
    private val audioProvider: AudioProvider,
    private val configManager: ConfigManager,
) {
    private val requestMutex = Mutex()

    suspend fun resolve(audioMediaConfig: AudioMediaConfig): AudioMediaConfig {
        val mediaId = audioMediaConfig.mediaId?.takeIf { it.isNotBlank() }
            ?: return audioMediaConfig
        if (!needsLionfish(audioMediaConfig)) return audioMediaConfig

        val lionfishMedia = requestMutex.withLock {
            download(mediaId)
        } ?: return audioMediaConfig

        return audioMediaConfig.merge(lionfishMedia)
    }

    fun needsLionfish(audioMediaConfig: AudioMediaConfig): Boolean =
        audioMediaConfig.streamUrl.isNullOrBlank() ||
                audioMediaConfig.streamUrlNoAds.isNullOrBlank() ||
                audioMediaConfig.title.isNullOrBlank() ||
                audioMediaConfig.primaryLabel.isNullOrBlank() ||
                audioMediaConfig.date == null ||
                audioMediaConfig.imageUrl.isNullOrBlank() ||
                audioMediaConfig.duration == null ||
                audioMediaConfig.series.isNullOrBlank() ||
                audioMediaConfig.seriesSlug.isNullOrBlank() ||
                audioMediaConfig.subscriptionLinks == null ||
                (audioMediaConfig.isVastEnabled == true && audioMediaConfig.adConfig == null)

    private suspend fun download(mediaId: String): LionfishMedia? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val baseUrl = audioProvider.getAudioApiBaseUrl().trimEnd('/')
            val url = URL("$baseUrl/audio/${Uri.encode(mediaId)}")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            headers.forEach { header ->
                connection.setRequestProperty(header.key, header.value)
            }
            if (connection.responseCode !in 200 until 300) {
                Logger.w(
                    TAG,
                    "Lionfish podcast metadata request failed: ${connection.responseCode}"
                )
                return@withContext null
            }
            connection.inputStream.bufferedReader().use { reader ->
                Gson().fromJson(reader, LionfishMedia::class.java)
            }
        } catch (exception: Exception) {
            Logger.w(TAG, "Lionfish podcast metadata request failed", exception)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun AudioMediaConfig.merge(lionfishMedia: LionfishMedia): AudioMediaConfig {
        val lionfishLinks = lionfishMedia.seriesMeta?.subscriptionLinks
        val mergedLinks = if (subscriptionLinks != null || lionfishLinks != null) {
            AudioMediaSubscriptionLinks(
                alexa = subscriptionLinks?.alexa ?: lionfishLinks?.alexa,
                applePodcasts = subscriptionLinks?.applePodcasts ?: lionfishLinks?.applePodcasts,
                googlePlay = subscriptionLinks?.googlePlay ?: lionfishLinks?.googlePlay,
                iheartRadio = subscriptionLinks?.iheartRadio ?: lionfishLinks?.iheartRadio,
                radioPublic = subscriptionLinks?.radioPublic ?: lionfishLinks?.radioPublic,
                rss = subscriptionLinks?.rss ?: lionfishLinks?.rss,
                spotify = subscriptionLinks?.spotify ?: lionfishLinks?.spotify,
                stitcher = subscriptionLinks?.stitcher ?: lionfishLinks?.stitcher,
                tuneIn = subscriptionLinks?.tuneIn ?: lionfishLinks?.tuneIn,
            )
        } else {
            null
        }
        val seriesName = lionfishMedia.seriesMeta?.seriesName

        return copy(
            streamUrl = streamUrl.takeUnless { it.isNullOrBlank() }
                ?: lionfishMedia.audio?.completeUrl,
            streamUrlNoAds = streamUrlNoAds.takeUnless { it.isNullOrBlank() }
                ?: lionfishMedia.audio?.podTracUrl,
            title = title.takeUnless { it.isNullOrBlank() } ?: lionfishMedia.title,
            primaryLabel = primaryLabel.takeUnless { it.isNullOrBlank() } ?: seriesName,
            date = date ?: lionfishMedia.publicationDate,
            imageUrl = imageUrl.takeUnless { it.isNullOrBlank() }
                ?: lionfishMedia.seriesMeta?.images?.coverImage?.url,
            duration = duration ?: lionfishMedia.duration,
            series = series.takeUnless { it.isNullOrBlank() }
                ?: lionfishMedia.series.takeUnless { it.isNullOrBlank() }
                ?: seriesName,
            seriesSlug = seriesSlug.takeUnless { it.isNullOrBlank() }
                ?: lionfishMedia.seriesMeta?.seriesSlug,
            podcastSlug = podcastSlug.takeUnless { it.isNullOrBlank() } ?: lionfishMedia.slug,
            subscriptionLinks = mergedLinks,
            adConfig = (adConfig ?: getDefaultAdConfig(getPlayerType()))
                ?.merge(getPlayerType(), lionfishMedia),
        )
    }

    private val AudioMediaConfig.isVastEnabled
        get() = configManager.config.adsConfig.audio.getConfig(getPlayerType())?.vastEnabled

    private fun getDefaultAdConfig(playerType: PlayerType): AudioMediaAdConfig? {
        val appConfig = configManager.config.adsConfig.audio.getConfig(playerType)
        return appConfig?.toAudioAdConfig()
    }

    private fun AudioMediaAdConfig.merge(
        playerType: PlayerType,
        lionfishMedia: LionfishMedia,
    ): AudioMediaAdConfig {
        val adBreaks = when {
            adBreaks != null -> adBreaks
            playerType == PODCAST -> {
                val appConfig = configManager.config.adsConfig.audio.getConfig(playerType)
                val result = mutableListOf<AudioMediaAdBreak>()
                appConfig?.adBreaks?.forEach { cfgAdBreak ->
                    if (cfgAdBreak.type.lowercase() == "midroll" && cfgAdBreak.timeSeconds == null) {
                        val midrollPointsSeconds = lionfishMedia.files?.firstOrNull()
                            ?.rhapsochord
                            ?.midRollPoints
                            ?.sorted()
                        midrollPointsSeconds?.forEach { timeSeconds ->
                            cfgAdBreak.toAdBreak(timeSeconds = timeSeconds.toInt())?.let {
                                result.add(it)
                            }
                        }
                    } else {
                        cfgAdBreak.toAdBreak()?.let {
                            result.add(it)
                        }
                    }
                }
                result
            }

            else -> null
        }

        return this.copy(
            adBreaks = adBreaks,
            contentLanguage = contentLanguage
                ?: lionfishMedia.language
                    ?.takeIf { it.length >= 2 }
                    ?.substring(0, 2),
            seriesName = seriesName
                ?: lionfishMedia.seriesMeta?.seriesName?.takeIf { playerType == PODCAST },
        )
    }

    private companion object {
        const val TAG = "PodcastMetadataResolver"
        const val TIMEOUT_MS = 5_000
    }
}

private class LionfishMedia {
    @SerializedName("id")
    var id: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("slug")
    var slug: String? = null

    @SerializedName("series")
    var series: String? = null

    @SerializedName("audio")
    var audio: LionfishAudio? = null

    @SerializedName("publicationDate")
    var publicationDate: Long? = null

    @SerializedName("duration")
    var duration: Long? = null

    @SerializedName("seriesMeta")
    var seriesMeta: LionfishSeriesMeta? = null

    @SerializedName("files")
    var files: List<LionfishFiles>? = null

    @SerializedName("language")
    var language: String? = null

}

private class LionfishAudio {
    @SerializedName("completeUrl")
    var completeUrl: String? = null

    @SerializedName("podTracUrl")
    var podTracUrl: String? = null
}

private class LionfishFiles {
    @SerializedName("rhapsochord")
    var rhapsochord: LionfishFilesRhapsochord? = null
}

private class LionfishFilesRhapsochord {
    @SerializedName("preRoll")
    var preRoll: Boolean? = null

    @SerializedName("postRoll")
    var postRoll: Boolean? = null

    @SerializedName("midRollPoints")
    var midRollPoints: List<Double>? = null
}

private class LionfishSeriesMeta {
    @SerializedName("seriesSlug")
    var seriesSlug: String? = null

    @SerializedName("seriesName")
    var seriesName: String? = null

    @SerializedName("images")
    var images: LionfishSeriesImages? = null

    @SerializedName("subscriptionLinks")
    var subscriptionLinks: LionfishSubscriptionLinks? = null
}

private class LionfishSeriesImages {
    @SerializedName("coverImage")
    var coverImage: LionfishSeriesImage? = null
}

private class LionfishSeriesImage {
    @SerializedName("url")
    var url: String? = null
}

private class LionfishSubscriptionLinks {
    @SerializedName("alexa")
    var alexa: String? = null

    @SerializedName("applePodcasts")
    var applePodcasts: String? = null

    @SerializedName("googlePlay")
    var googlePlay: String? = null

    @SerializedName("iheartRadio")
    var iheartRadio: String? = null

    @SerializedName("radioPublic")
    var radioPublic: String? = null

    @SerializedName("rss")
    var rss: String? = null

    @SerializedName("spotify")
    var spotify: String? = null

    @SerializedName("stitcher")
    var stitcher: String? = null

    @SerializedName("tuneIn")
    var tuneIn: String? = null
}

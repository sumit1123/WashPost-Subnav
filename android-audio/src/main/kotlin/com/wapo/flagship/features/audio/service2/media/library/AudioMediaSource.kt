/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship.features.audio.service2.media.library

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.gson.Gson
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor.Companion.headers
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.isVastEnabled
import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepository
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.putExtra
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfigList
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.models.PlaybackVoice
import com.wapo.flagship.features.audio.models.Voices
import com.wapo.flagship.features.audio.podcast.PodcastMetadataResolver
import com.wapo.flagship.features.audio.service2.media.extensions.*
import com.wapo.flagship.features.audio.service2.media.extensions.toUri
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.audio.utils.validMimeTypes
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.washingtonpost.android.config.domain.manager.ConfigManager
import kotlinx.coroutines.*
import java.lang.IllegalStateException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Source of [MediaMetadataCompat] objects created from various JSON streams.
 */
class AudioMediaSource(
    private val context: Context,
    private val audioProvider: AudioProvider,
    val audioMediaConfigList: AudioMediaConfigList,
    private val adsRepository: AudioAdsRepository,
    private val podcastMetadataResolver: PodcastMetadataResolver,
    private val configStateCallback: (String, AudioPlaybackState) -> Unit,
    private val mediaSourceStateCallback: (AudioPlaybackState) -> Unit,
    private var catalogMap: LinkedHashMap<String, List<MediaItem>> = LinkedHashMap(),
    private var catalog: List<MediaItem> = emptyList(),
) : AbstractMusicSource() {

    private val tag = "AudioMediaSource"

    private val sourceJob = SupervisorJob()
    private val sourceScope = CoroutineScope(Dispatchers.Main + sourceJob)

    private val audioAdsConfig get() = ConfigManager.getInstance().config.adsConfig.audio

    fun copy(): AudioMediaSource {
        return AudioMediaSource(
            context,
            audioProvider,
            audioMediaConfigList,
            adsRepository,
            podcastMetadataResolver,
            configStateCallback,
            mediaSourceStateCallback,
            catalogMap,
            catalog,
        ).apply {
            state = STATE_INITIALIZED
        }
    }

    init {
        sourceScope.launch {
            load()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioMediaSource) return false

        if (context != other.context) return false
        if (audioProvider != other.audioProvider) return false
        if (audioMediaConfigList != other.audioMediaConfigList) return false
        if (podcastMetadataResolver != other.podcastMetadataResolver) return false
        if (configStateCallback != other.configStateCallback) return false
        if (mediaSourceStateCallback != other.mediaSourceStateCallback) return false
        if (catalogMap != other.catalogMap) return false
        if (catalog != other.catalog) return false

        return true
    }

    override fun hashCode(): Int {
        return Int.hashCode()
    }

    override fun iterator(): Iterator<MediaItem> = catalog.iterator()

    override suspend fun load() {
        if (state == STATE_INITIALIZED) return
        state = STATE_INITIALIZING
        mediaSourceStateCallback.invoke(AudioPlaybackState.JSONSourceInitializing)
        // Make a copy to avoid ConcurrentModificationException
        val configs = audioMediaConfigList.list.toList()
        configs.forEach { audioMediaConfig ->
            Logger.d(
                tag,
                "AudioDebug, Processing, type=${audioMediaConfig.getPlayerType()}, id=${audioMediaConfig.id}"
            )
            if (audioMediaConfig.getPlayerType() == PlayerType.UNKNOWN) {
                EventLog.Builder().apply {
                    setMessage("AudioMediaConfig PlayerType is Unknown")
                    setModule(LogModules.AUDIO)
                    set("media_id", audioMediaConfig.mediaId)
                    set("content_url", audioMediaConfig.contentUrl)
                }.run {
                    audioProvider.onError(context, this)
                }
            }
            configStateCallback.invoke(
                audioMediaConfig.id,
                AudioPlaybackState.JSONSourceInitializing
            )
            val mediaMetaDataList = loadMedia(audioMediaConfig)
            catalogMap[audioMediaConfig.id] = mediaMetaDataList
            configStateCallback.invoke(
                audioMediaConfig.id,
                if (catalogMap[audioMediaConfig.id].isNullOrEmpty())
                    AudioPlaybackState.JSONSourceError
                else
                    AudioPlaybackState.JSONSourceInitialized
            )
        }
        catalog = catalogMap.flatMap { it.value }
        mediaSourceStateCallback.invoke(
            if (catalog.isNotEmpty())
                AudioPlaybackState.JSONSourceInitialized
            else
                AudioPlaybackState.JSONSourceError
        )
        state = if (catalog.isNotEmpty()) STATE_INITIALIZED else STATE_ERROR
        Logger.d(
            tag,
            "AudioDebug, Processed, configsCount=${audioMediaConfigList.list.size}, catalogCount=${catalog.size}"
        )
    }

    suspend fun loadMedia(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        Logger.d(
            tag,
            "AudioDebug, Processing, type=${audioMediaConfig.getPlayerType()}, id=${audioMediaConfig.id}"
        )
        if (audioMediaConfig.audioType == PLACEHOLDER) {
            Logger.d(tag, "AudioDebug, Creating placeholder MediaItem, id=${audioMediaConfig.id}")
            val mediaMetaData = MediaMetadata.Builder()
                .from(audioMediaConfig)
                .build()

            // build media item without uri
            val mediaItem = MediaItem.Builder().apply {
                setMediaId(audioMediaConfig.id)
                setTag(audioMediaConfig)
                setMediaMetadata(mediaMetaData)
            }.build()
            return listOf(mediaItem)
        }
        // Resolve ad state before loading any resolving any stream urls.
        audioMediaConfig.resolveAdState(
            audioProvider.getAudioAdInterval(),
            audioProvider.shouldSuppressAds()
        )
        return when (audioMediaConfig.getPlayerType()) {
            PlayerType.PODCAST -> {
                if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(audioMediaConfig.audioType)) {
                    loadPersoPodcast(audioMediaConfig)
                } else {
                    loadPodcast(audioMediaConfig)
                }
            }

            PlayerType.AUTOMATED, PlayerType.STANDALONE -> {
                loadAutomatedAudio(audioMediaConfig)
            }

            PlayerType.HUMAN -> {
                loadHumanAudio(audioMediaConfig)
            }

            else -> {
                emptyList()
            }
        }
    }

    fun addMediaToCatalog(id: String, mediaList: List<MediaItem>) {
        catalogMap[id] = mediaList
        catalog = catalogMap.flatMap { it.value }
    }

    fun removeMediaFromCatalog(id: String) {
        catalogMap.remove(id)
        catalog = catalogMap.flatMap { it.value }
    }

    fun getMedia(audioMediaConfig: AudioMediaConfig): List<MediaItem>? {
        return catalogMap[audioMediaConfig.id]
    }

    fun getCatalog(): List<MediaItem> {
        return catalog
    }

    private suspend fun loadChildrenAutomated(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        val playlist = mutableListOf<MediaItem>()
        val children = mutableListOf<MediaItem>()

        // build children media items
        audioMediaConfig.children?.forEach { child ->
            children.addAll(loadAutomatedAudio(child))
        }

        // build transition + parent media items
        val overview =
            loadAutomatedAudio(audioMediaConfig.copy(children = null, transitions = null))
        val outro =
            audioMediaConfig.transitions?.find { it.type == OUTRO }?.let { loadAutomatedAudio(it) }

        // assemble luf media item playlist
        playlist.addAll(overview)
        outro?.let { playlist.addAll(it) }
        children.let { playlist.addAll(it) }

        val durations = playlist.map { it.mediaMetadata.duration ?: 0L }

        // build placeholder luf for UI
        val uris = playlist.mapNotNull { it.localConfiguration?.uri?.toString() }
        val metadataBuilder = MediaMetadata.Builder().from(audioMediaConfig)

        val existingExtras = metadataBuilder.build().extras ?: Bundle()
        val combinedExtras = Bundle(existingExtras).apply {
            putStringArrayList(CONCAT_CHILDREN_URIS, ArrayList(uris))
            putStringArrayList(
                CONCAT_CHILDREN_DURATIONS,
                ArrayList(durations.map { it.toString() })
            )
            putBoolean(IS_CONCAT2, true)
        }

        metadataBuilder
            .setExtras(combinedExtras)

        val placeholder = MediaItem.Builder()
            .setMediaId(audioMediaConfig.id)
            .setMediaMetadata(metadataBuilder.build())
            .build()

        return listOf(placeholder)
    }

    private suspend fun loadAutomatedAudio(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        if (audioMediaConfig.children != null || audioMediaConfig.transitions != null) {
            return loadChildrenAutomated(audioMediaConfig)
        }
        // Load voices if they are not loaded yet
        if (audioMediaConfig.voices.isNullOrEmpty() && !audioMediaConfig.manifestUrl.isNullOrEmpty()) {
            val voices = loadManifestVoices(audioMediaConfig.manifestUrl.toUri())
            val playbackVoices = mutableListOf<PlaybackVoice>()
            voices.voices?.forEach {
                if (it.id != null && it.rawUrl != null && it.label != null) {
                    // JsonApp will send duration in seconds. But Manifest has duration in millis.
                    // Converting duration to seconds in PlaybackVoice to keep it consistent.
                    val durationInSeconds = it.duration?.run {
                        (if (it.duration > 0) (it.duration / 1000) else it.duration).toLong()
                    }
                    PlaybackVoice(
                        it.id,
                        it.label,
                        it.rawUrl,
                        it.adsUrl,
                        durationInSeconds
                    ).also { playbackVoices.add(it) }
                }
            }
            audioMediaConfig.voices = playbackVoices
        }
        // find resolvedMediaUrl of a first voice based on url head request
        val firstPlaybackVoice = AudioPreferences.getPreferredPlaybackVoice(audioMediaConfig.voices)
        var resolvedMediaUrl: String? = null
        var resolvedMediaAdsUrl: String? = null
        if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
            resolvedMediaUrl = firstPlaybackVoice?.rawUrl ?: audioMediaConfig.rawUrl
                    ?: audioMediaConfig.url
            resolvedMediaAdsUrl = null
        } else {
            resolvedMediaUrl = firstPlaybackVoice?.run {
                if (audioMediaConfig.getPlayAd() && !adsUrl.isNullOrEmpty()) {
                    resolvedMediaAdsUrl = adsUrl
                    adsUrl
                } else rawUrl
            } ?: if (audioMediaConfig.getPlayAd() && !audioMediaConfig.adsUrl.isNullOrEmpty()) {
                resolvedMediaAdsUrl = audioMediaConfig.adsUrl
                audioMediaConfig.adsUrl
            } else if (!audioMediaConfig.rawUrl.isNullOrEmpty()) {
                audioMediaConfig.rawUrl
            } else {
                audioMediaConfig.url
            }
        }
        audioMediaConfig.resolvedMediaUrl = resolvedMediaUrl
        audioMediaConfig.resolvedAdsUrl =
            appendAdCustomTargetingValues(audioMediaConfig.adsCustomTargeting, resolvedMediaAdsUrl)
        if (resolvedMediaUrl != null) {
            val mediaMetaData =
                MediaMetadata.Builder().from(audioMediaConfig).build()

            val mediaItem = MediaItem.Builder().apply {
                setMediaId(audioMediaConfig.id)
                if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
                    setUri(resolvedMediaUrl.toUri())
                    val adsConfig = adsRepository.getAdsConfig(audioMediaConfig)
                    putExtra(mediaMetaData) {
                        putParcelable(METADATA_KEY_ADS_CONFIG, adsConfig)
                    }
                } else {
                    if (!audioMediaConfig.resolvedAdsUrl.isNullOrEmpty() && audioMediaConfig.getPlayAd()) {
                        setUri(audioMediaConfig.resolvedAdsUrl.toUri())
                        EventLog.Builder().apply {
                            setMessage("Requesting Audio Ads URL")
                            set("ads_url", audioMediaConfig.resolvedAdsUrl)
                            setModule(LogModules.AUDIO)
                        }.run {
                            audioProvider.debugLog(context, this)
                        }
                    } else {
                        setUri(resolvedMediaUrl.toUri())
                    }
                }
                setTag(audioMediaConfig)
                setMediaMetadata(mediaMetaData)
            }.build()

            return listOf(mediaItem)
        }
        return emptyList()
    }

    private suspend fun loadHumanAudio(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        var resolvedMediaUrl: String? = null
        var resolvedMediaAdsUrl: String? = null
        if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
            resolvedMediaUrl =
                if (!audioMediaConfig.humanRawUrl.isNullOrEmpty()) {
                    audioMediaConfig.humanRawUrl
                } else {
                    audioMediaConfig.rawUrl
                }
        } else {
            resolvedMediaUrl =
                if (audioMediaConfig.getPlayAd() && !audioMediaConfig.humanAdsUrl.isNullOrEmpty()) {
                    resolvedMediaAdsUrl = audioMediaConfig.humanAdsUrl
                    audioMediaConfig.humanAdsUrl
                } else if (!audioMediaConfig.humanRawUrl.isNullOrEmpty())
                    audioMediaConfig.humanRawUrl
                else if (audioMediaConfig.getPlayAd() && !audioMediaConfig.adsUrl.isNullOrEmpty()) {
                    resolvedMediaAdsUrl = audioMediaConfig.adsUrl
                    audioMediaConfig.adsUrl
                } else
                    audioMediaConfig.rawUrl
        }
        audioMediaConfig.resolvedMediaUrl = resolvedMediaUrl
        audioMediaConfig.resolvedAdsUrl =
            appendAdCustomTargetingValues(audioMediaConfig.adsCustomTargeting, resolvedMediaAdsUrl)
        if (resolvedMediaUrl != null) {
            // prepare MediaMetadata
            val mediaMetaData =
                MediaMetadata.Builder().from(audioMediaConfig).build()

            val mediaItem = MediaItem.Builder().apply {
                setMediaId(audioMediaConfig.id)
                if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
                    setUri(resolvedMediaUrl.toUri())
                    val adsConfig = adsRepository.getAdsConfig(audioMediaConfig)
                    putExtra(mediaMetaData) {
                        putParcelable(METADATA_KEY_ADS_CONFIG, adsConfig)
                    }
                } else {
                    if (!audioMediaConfig.resolvedAdsUrl.isNullOrEmpty() && audioMediaConfig.getPlayAd()) {
                        setUri(audioMediaConfig.resolvedAdsUrl.toUri())
                        EventLog.Builder().apply {
                            setModule(LogModules.AUDIO)
                            setMessage("Requesting Audio Ads URL")
                            set("ads_url", audioMediaConfig.resolvedAdsUrl)
                        }.run {
                            audioProvider.debugLog(context, this)
                        }
                    } else {
                        setUri(resolvedMediaUrl.toUri())
                    }
                }
                setTag(audioMediaConfig)
                setMediaMetadata(mediaMetaData)
            }.build()

            return listOf(mediaItem)
        }
        return emptyList()
    }

    private fun loadPersoPodcast(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        val mediaItems = ArrayList<MediaItem>()
        return try {
            val baseMetadata = MediaMetadata.Builder()
                .from(audioMediaConfig)
                .build()
            baseMetadata.extras?.apply {
                putString(METADATA_KEY_SERIES_SLUG, audioMediaConfig.audioType)
                putString(METADATA_KEY_PODCAST_SLUG, audioMediaConfig.primaryLabel)
                putBoolean(IS_SHARED_PODCAST, audioMediaConfig.isShared == true)
            }
            val mediaItem = MediaItem.Builder().apply {
                setMediaId(audioMediaConfig.id)
                setUri(audioMediaConfig.streamUrl.toUri())
                setTag(audioMediaConfig)
                setMediaMetadata(baseMetadata)
            }
            mediaItems.add(mediaItem.build())
            mediaItems
        } catch (e: Exception) {
            onPodcastError(e, audioMediaConfig)
            mediaItems
        }
    }

    private suspend fun loadPodcast(audioMediaConfig: AudioMediaConfig): List<MediaItem> {
        val mediaItems = ArrayList<MediaItem>()
        return try {
            val resolvedAudioMediaConfig = podcastMetadataResolver.resolve(audioMediaConfig)
                .also { resolvedConfig ->
                    if (resolvedConfig != audioMediaConfig) {
                        val configIndex =
                            audioMediaConfigList.list.indexOfFirst { it === audioMediaConfig }
                                .takeIf { it >= 0 }
                                ?: audioMediaConfigList.list.indexOfFirst {
                                    it.id == audioMediaConfig.id
                                }
                        configIndex
                            .takeIf { it >= 0 }
                            ?.let { audioMediaConfigList.list[it] = resolvedConfig }
                    }
                }

            val isVastEnabled = audioAdsConfig.isVastEnabled(resolvedAudioMediaConfig)
            val useAdFreeUrl = audioProvider.shouldSuppressAds() || isVastEnabled
            val mediaUrl = if (useAdFreeUrl) {
                resolvedAudioMediaConfig.streamUrlNoAds ?: resolvedAudioMediaConfig.streamUrl
            } else {
                resolvedAudioMediaConfig.streamUrl ?: resolvedAudioMediaConfig.streamUrlNoAds
            }
            if (mediaUrl.isNullOrBlank()) {
                throw IllegalStateException("Podcast stream URL must not be null or blank")
            }
            resolvedAudioMediaConfig.resolvedMediaUrl = mediaUrl

            val baseMetadata = MediaMetadata.Builder()
                .from(resolvedAudioMediaConfig)
                .build()
            baseMetadata.extras?.apply {
                putString(METADATA_KEY_DISPLAY_ICON_URI, resolvedAudioMediaConfig.imageUrl)
                putString(METADATA_KEY_DISPLAY_SUBTITLE, resolvedAudioMediaConfig.primaryLabel)
                putString(METADATA_KEY_SERIES_SLUG, resolvedAudioMediaConfig.seriesSlug)
                putString(METADATA_KEY_PODCAST_SLUG, resolvedAudioMediaConfig.podcastSlug)
                putString(METADATA_SUBSCRIPTION_LINKS, mutableListOf<String>().apply {
                    resolvedAudioMediaConfig.subscriptionLinks?.let { links ->
                        links.alexa?.let {
                            add(PodcastLinkType.ALEXA.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.googlePlay?.let {
                            add(PodcastLinkType.GOOGLE_PLAY.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.applePodcasts?.let {
                            add(PodcastLinkType.APPLE_PODCASTS.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.iheartRadio?.let {
                            add(PodcastLinkType.I_HEART_RADIO.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.radioPublic?.let {
                            add(PodcastLinkType.RADIO_PUBLIC.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.rss?.let {
                            add(PodcastLinkType.RSS.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.spotify?.let {
                            add(PodcastLinkType.SPOTIFY.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.stitcher?.let {
                            add(PodcastLinkType.STITCHER.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                        links.tuneIn?.let {
                            add(PodcastLinkType.TUNE_IN.linkName + SUBSCRIPTION_LINK_DELIMITER + it)
                        }
                    }
                }.joinToString(separator = METADATA_LIST_DELIMITER))
            }

            val mediaItem = MediaItem.Builder().apply {
                setMediaId(resolvedAudioMediaConfig.id)
                setUri(mediaUrl.toUri())
                if (isVastEnabled) {
                    val adsConfig = adsRepository.getAdsConfig(resolvedAudioMediaConfig)
                    if (adsConfig != null) {
                        putExtra(baseMetadata) {
                            putParcelable(METADATA_KEY_ADS_CONFIG, adsConfig)
                        }
                    }
                }
                setTag(resolvedAudioMediaConfig)
                setMediaMetadata(baseMetadata)
            }
            mediaItems.add(mediaItem.build())
            mediaItems
        } catch (e: Exception) {
            onPodcastError(e, audioMediaConfig)
            mediaItems
        }
    }

    private fun onPodcastError(e: Exception, audioMediaConfig: AudioMediaConfig) {
        audioProvider.onPodcastEvent(AudioProvider.EventType.ON_ERROR, null, e)
        EventLog.Builder().apply {
            setMessage("Podcast Media Load Error")
            setErrorMessage(e.message)
            setContentUrl(audioMediaConfig.contentUrl)
            setModule(LogModules.AUDIO)
        }.run {
            audioProvider.onError(context, this)
        }
    }

    private suspend fun loadManifestVoices(uri: Uri?): Voices {
        val voices = Voices(emptyList())
        return try {
            withContext(Dispatchers.IO) {
                downloadManifestVoices(uri).also { manifestVoices ->
                    if (manifestVoices.voices.isNullOrEmpty()) {
                        EventLog.Builder().apply {
                            setMessage("Manifest voices are empty or null")
                            setModule(LogModules.AUDIO)
                            set("manifest_url", uri?.toString())
                            set("voices_empty", manifestVoices.voices?.isEmpty())
                        }.run {
                            audioProvider.onError(context, this)
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            EventLog.Builder().apply {
                setMessage("Polly Json Load Error")
                setErrorMessage(e.message)
                setContentUrl(uri?.toString())
                setModule(LogModules.AUDIO)
            }.run {
                audioProvider.onError(context, this)
            }
            voices
        }
    }

    private suspend fun appendAdCustomTargetingValues(
        params: Map<String, List<String>>?,
        adsUrl: String?
    ): String? {
        if (params.isNullOrEmpty()) return null
        return if (!adsUrl.isNullOrEmpty()) {
            val urlBuilder = StringBuilder(adsUrl)
            if (!adsUrl.contains("?")) {
                urlBuilder.append("?")
            } else if (!adsUrl.endsWith("?")) {
                urlBuilder.append("&")
            }

            params.forEach { (key, value) ->
                val encodedKey = URLEncoder.encode(key, "UTF-8")
                val encodedValue = URLEncoder.encode(value.joinToString(separator = ",") { it }, "UTF-8")
                urlBuilder.append("$encodedKey=$encodedValue&")
            }

            // Remove the last '&' character
            if (urlBuilder.endsWith("&")) {
                urlBuilder.deleteCharAt(urlBuilder.length - 1)
            }

            return urlBuilder.toString()
        } else null
    }

    @Throws(Exception::class)
    private fun downloadManifestVoices(uri: Uri?): Voices {
        val json = URL(uri.toString()).readText()
        return Gson().fromJson(json, Voices::class.java)
    }

    private suspend fun isUrlHeadWorking(uri: Uri?): Boolean {
        if (uri?.toString().isNullOrEmpty()) return false
        return withContext(Dispatchers.IO) {
            var httpURLConnection: HttpURLConnection? = null
            try {
                val url = URL(uri.toString())
                httpURLConnection = url.openConnection() as HttpURLConnection
                httpURLConnection.requestMethod = "HEAD"
                httpURLConnection.connectTimeout = 5000
                httpURLConnection.readTimeout = 5000
                for (header in headers) {
                    httpURLConnection.setRequestProperty(header.key, header.value)
                }
                httpURLConnection.responseCode in 200 until 300 && validMimeTypes.contains(
                    httpURLConnection.contentType
                )
            } catch (e: Exception) {
                false
            } finally {
                httpURLConnection?.disconnect()
            }
        }
    }
}

@Throws(KotlinNullPointerException::class)
fun MediaMetadata.Builder.from(config: AudioMediaConfig): MediaMetadata.Builder {

    val extras = Bundle()
    extras.putString(METADATA_KEY_MEDIA_ID, config.id)
    extras.putString(METADATA_KEY_DISPLAY_TITLE_PREFIX, config.titlePrefix)
    extras.putString(METADATA_KEY_DISPLAY_TITLE_SEPARATOR, config.titleSeparator)
    extras.putString(METADATA_KEY_DISPLAY_LABEL_PRIMARY, config.primaryLabel)
    extras.putString(METADATA_KEY_DISPLAY_LABEL_SECONDARY, config.secondaryLabel)
    extras.putString(METADATA_KEY_DISPLAY_LABEL_STYLE, config.labelStyle)
    this.setDisplayTitle(config.title)
    this.setTitle(config.title)
    extras.putString(
        METADATA_KEY_DISPLAY_SUBTITLE,
        config.subtitle ?: config.titlePrefix ?: config.sectionName ?: ""
    )
    extras.putString(METADATA_KEY_PLAYER_TYPE, config.getPlayerType().name)
    extras.putLong(METADATA_KEY_DURATION, config.duration ?: 0)
    extras.putString(METADATA_KEY_DATE, config.date?.toString())
    extras.putString(METADATA_KEY_IMAGE_URL, config.imageUrl)
    extras.putString(METADATA_KEY_IMAGE_CAPTION, config.imageCaption)
    extras.putString(METADATA_KEY_CAPTION, config.caption)
    extras.putString(METADATA_KEY_CONTENT_URL, config.contentUrl)
    extras.putString(METADATA_KEY_SECTION_NAME, config.sectionName)
    extras.putString(METADATA_KEY_AUDIO_TYPE, config.audioType)

    extras.putString(METADATA_KEY_ALBUM_ART_URI, config.imageUrl)

    val resolvedMediaUrl = config.resolvedMediaUrl
    extras.putString(METADATA_KEY_MEDIA_URI, resolvedMediaUrl)
    extras.putString(METADATA_KEY_MEDIA_ADS_URI, config.resolvedAdsUrl)
    extras.putString(METADATA_KEY_PLAY_AD, if (config.getPlayAd()) "T" else "F")
    //TODO need to pass resizer url here
    this.setArtworkUri(config.imageUrl?.toUri())
    this.setExtras(extras)
    this.setIsPlayable(true)
    this.setIsBrowsable(false)
    return this
}

enum class PodcastLinkType(val linkName: String) {
    ALEXA("Alexa"),
    APPLE_PODCASTS("Apple Podcasts"),
    GOOGLE_PLAY("Google Podcasts"),
    I_HEART_RADIO("iHeartRadio"),
    RADIO_PUBLIC("RadioPublic"),
    RSS("RSS"),
    SPOTIFY("Spotify"),
    STITCHER("Stitcher"),
    TUNE_IN("TuneIn")
}

const val SUBSCRIPTION_LINK_DELIMITER = "@JSON@"
const val IS_CONCAT2 = "is_concat2"
const val CONCAT_CHILDREN_URIS = "concat_children_uris"
const val CONCAT_CHILDREN_DURATIONS = "concat_children_durations"
const val OUTRO = "outro"

package com.wapo.flagship.features.audio.playlist

import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.podcast.PodcastMetadataResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val playlistDatabase: PlaylistDatabase,
    private val podcastMetadataResolver: PodcastMetadataResolver,
) {

    suspend fun addPlaylistAudio(playlistAudio: Playlist): Boolean {
        val resolvedPlaylist = if (playlistAudio.playerType == PlayerType.PODCAST.name) {
            val resolvedConfig = podcastMetadataResolver.resolve(playlistAudio.toPodcastMediaConfig())
            playlistAudio.withPodcastMetadata(resolvedConfig)
        } else {
            playlistAudio
        }
        if (
            resolvedPlaylist.playerType == PlayerType.PODCAST.name &&
            resolvedPlaylist.streamUrl.isNullOrBlank() &&
            resolvedPlaylist.streamUrlNoAds.isNullOrBlank()
        ) {
            return false
        }
        withContext(Dispatchers.IO) {
            playlistDatabase.playlistDao().insertPlaylistAudio(resolvedPlaylist)
        }
        return true
    }

    suspend fun removePlaylistAudio(playlistAudio: Playlist) {
        withContext(Dispatchers.IO) {
            playlistDatabase.playlistDao().deletePlaylistAudio(playlistAudio.id)
        }
    }

    suspend fun removePlaylistByIdAudio(id: String) {
        withContext(Dispatchers.IO) {
            playlistDatabase.playlistDao().deletePlaylistAudio(id)
        }
    }

    suspend fun getPlaylistArticleExists(id: String) : Boolean{
        return withContext(Dispatchers.IO) {
            playlistDatabase.playlistDao().getPlaylistArticalExists(id)
        }
    }

    fun getPlaylist(): Flow<List<Playlist>> {
        return playlistDatabase.playlistDao().getPlaylist()
            .flowOn(Dispatchers.IO)
            .conflate()
    }

    suspend fun clearPlaylist() {
        withContext(Dispatchers.IO) {
            playlistDatabase.playlistDao().cleanUp()
        }
    }

    private fun Playlist.toPodcastMediaConfig(): AudioMediaConfig =
        AudioMediaConfig(
            mediaId = mediaId,
            streamUrl = streamUrl,
            streamUrlNoAds = streamUrlNoAds,
            title = title,
            primaryLabel = primaryLabel ?: subtitle,
            date = date,
            imageUrl = imageUrl,
            duration = duration,
            series = series,
            adConfig = adConfig,
            seriesSlug = seriesSlug,
            podcastSlug = podcastSlug,
            subscriptionLinks = subscriptionLinks,
        )

    private fun Playlist.withPodcastMetadata(config: AudioMediaConfig): Playlist =
        Playlist(
            playerType = playerType,
            id = id,
            mediaId = mediaId,
            humanAdsUrl = humanAdsUrl,
            humanRawUrl = humanRawUrl,
            manifestUrl = manifestUrl,
            adsUrl = adsUrl,
            rawUrl = rawUrl,
            title = config.title,
            titlePrefix = titlePrefix,
            titleSeparator = titleSeparator,
            subtitle = subtitle.takeUnless { it.isNullOrBlank() } ?: config.primaryLabel,
            date = config.date,
            imageUrl = config.imageUrl,
            imageCaption = imageCaption,
            duration = config.duration,
            streamUrl = config.streamUrl,
            streamUrlNoAds = config.streamUrlNoAds,
            contentUrl = contentUrl,
            sectionName = sectionName,
            caption = caption,
            labelType = labelType,
            primaryLabel = config.primaryLabel,
            secondaryLabel = secondaryLabel,
            voices = voices,
            arcId = arcId,
            tracker = tracker,
            primaryLabelStyle = primaryLabelStyle,
            series = config.series,
            adConfig = config.adConfig,
            seriesSlug = config.seriesSlug,
            podcastSlug = config.podcastSlug,
            subscriptionLinks = config.subscriptionLinks,
        )
}

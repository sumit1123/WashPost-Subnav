package com.wapo.flagship.di.app.modules.features.audio

import com.wapo.flagship.features.audio.playlist.PlaylistDatabase
import com.wapo.flagship.features.audio.playlist.PlaylistRepository
import com.wapo.flagship.features.audio.podcast.PodcastMetadataResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PlaylistRepositoryModule {
    @Singleton
    @Provides
    fun provideAudioPlaylistRepository(
        playlistDatabase: PlaylistDatabase,
        podcastMetadataResolver: PodcastMetadataResolver,
    ): PlaylistRepository = PlaylistRepository(playlistDatabase, podcastMetadataResolver)
}

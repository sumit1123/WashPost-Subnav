package com.wapo.flagship.features.personalizedpodcasts.repo

import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigResponse
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.personalizedpodcasts.model.TranscriptResponse
import okhttp3.RequestBody

interface PersonalizedPodcastRepository {
    suspend fun getPodcastConfigs(): PodcastConfigResponse?
    suspend fun getPodcasts(): List<PersonalizedPodcast>?
    suspend fun generatePodcast(requestBody: RequestBody?): PersonalizedPodcast?
    suspend fun fetchTranscript(transcriptUrl: String?): TranscriptResponse?
    suspend fun getPodcastMetadata(podcastId: String): PodcastResult
    fun getHeaders(): HashMap<String, String>
}

sealed class PodcastResult {
    data class Success(val podcast: PersonalizedPodcast) : PodcastResult()
    data object Error : PodcastResult()
    data object PodcastExpired : PodcastResult()
}
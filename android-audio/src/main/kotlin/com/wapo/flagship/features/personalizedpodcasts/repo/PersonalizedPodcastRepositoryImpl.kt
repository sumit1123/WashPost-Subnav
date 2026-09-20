package com.wapo.flagship.features.personalizedpodcasts.repo

import android.content.Context
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.personalizedpodcasts.model.PodcastConfigResponse
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.personalizedpodcasts.model.TranscriptResponse
import com.wapo.flagship.features.personalizedpodcasts.service.PersonalizedPodcastService
import com.washingtonpost.userhistory.network.APIResult
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.RequestBody
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

class PersonalizedPodcastRepositoryImpl @Inject constructor(
    @ApplicationContext val context: Context,
    private val personalizedPodcastService: PersonalizedPodcastService,
    private val audioProvider: AudioProvider
) : PersonalizedPodcastRepository {
    override suspend fun getPodcastConfigs(): PodcastConfigResponse? {
        val response = personalizedPodcastService.getPodcastConfigs(getHeaders())
        return when (response) {
            is APIResult.Failure -> {
                EventLog.Builder().apply {
                    setMessage("Fetching personalized podcasts failed")
                    setModule(LogModules.PERSONALIZED_PODCASTS)
                    setErrorMessage(response.getMessage())
                }.run {
                    audioProvider.onError(context, this)
                }
                null
            }

            is APIResult.NetworkError -> {
                null
            }

            is APIResult.Success -> {
                response.data
            }
        }
    }

    override suspend fun getPodcasts(): List<PersonalizedPodcast>? {
        val response = personalizedPodcastService.getUserGeneratedPodcasts(getHeaders())
        return when (response) {
            is APIResult.Failure -> {
                EventLog.Builder().apply {
                    setMessage("Fetching personalized podcasts failed")
                    setModule(LogModules.PERSONALIZED_PODCASTS)
                    setErrorMessage(response.getMessage())
                }.run {
                    audioProvider.onError(context, this)
                }
                null
            }

            is APIResult.NetworkError -> {
                null
            }

            is APIResult.Success -> {
                return response.data?.personalizedPodcasts
                    ?: run {
                        EventLog.Builder().apply {
                            setMessage("Personalized podcasts config data null")
                            setModule(LogModules.PERSONALIZED_PODCASTS)
                            setErrorMessage(response.getMessage())
                        }.run {
                            audioProvider.onError(context, this)
                        }
                        null
                    }
            }
        }
    }

    override suspend fun generatePodcast(requestBody: RequestBody?): PersonalizedPodcast? {
        val response = personalizedPodcastService.generatePodcast(getHeaders(), requestBody)
        return when (response) {
            is APIResult.Failure -> {
                EventLog.Builder().apply {
                    setMessage("Generating podcasts failed")
                    setModule(LogModules.PERSONALIZED_PODCASTS)
                    setErrorMessage(response.getMessage())
                }.run {
                    audioProvider.onError(context, this)
                }
                null
            }

            is APIResult.NetworkError -> {
                null
            }

            is APIResult.Success -> {
                response.data ?: run {
                    EventLog.Builder().apply {
                        setMessage("Generating podcasts response data null")
                        setModule(LogModules.PERSONALIZED_PODCASTS)
                        setErrorMessage(response.getMessage())
                    }.run {
                        audioProvider.onError(context, this)
                    }
                    null
                }
            }
        }
    }

    override suspend fun fetchTranscript(transcriptUrl: String?): TranscriptResponse? {
        return transcriptUrl?.let { url ->
            val isUrlInvalid = try {
                URL(url)
                false
            } catch (e: MalformedURLException) {
                true
            }

            if (isUrlInvalid) {
                handleError("Invalid Transcript URL", "Malformed URL provided: $url")
                null
            } else {
                val response = personalizedPodcastService.getTranscript(url)

                when (response) {
                    is APIResult.Success -> {
                        response.data
                    }
                    is APIResult.Failure -> {
                        handleError("Fetching transcript failed", response.getMessage())
                        null
                    }
                    is APIResult.NetworkError -> {
                        null
                    }
                }
            }
        }
    }

    private fun handleError(message: String, errorMessage: String?) {
        EventLog.Builder().apply {
            setMessage(message)
            setModule(LogModules.PERSONALIZED_PODCASTS)
            setErrorMessage(errorMessage)
        }.run {
            audioProvider.onError(context, this)
        }
    }

    override suspend fun getPodcastMetadata(podcastId: String): PodcastResult {
        val response = personalizedPodcastService.getPodcastMetadata(getHeaders(), podcastId)
        return when (response) {
            is APIResult.Success -> {
                response.data?.let {
                    PodcastResult.Success(it)
                } ?: run {
                    PodcastResult.Error
                }
            }

            is APIResult.Failure -> {
                when (response.statusCode) {
                    404 ->
                        PodcastResult.PodcastExpired

                    else ->
                        PodcastResult.Error
                }
            }

            is APIResult.NetworkError -> {
                PodcastResult.Error
            }
        }
    }

    override fun getHeaders(): HashMap<String, String> {
        return audioProvider.getUserHeaders()
    }
}
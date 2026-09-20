package com.wapo.flagship.features.articles2.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wapo.flagship.features.articles2.services.VoicesService
import com.wapo.flagship.features.articles2.states.AudioAvailableVoicesApiState
import com.wapo.flagship.network.retrofit.network.APIResult
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URL
import javax.inject.Inject

/**
 * This repository is used to download list of available voices for audio articles.
 * [okHttpClient] - Injected from dagger
 * [callAdapterFactory] - This is used to convert the response of the request in to appropriate [APIResult] states
 */
class VoicesRepository
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val callAdapterFactory: CallAdapter.Factory,
    ) {
        suspend fun startDownloadingAvailableVoices(url: String): LiveData<AudioAvailableVoicesApiState> {
            val liveData = MutableLiveData<AudioAvailableVoicesApiState>()
            try {
                val urlObj = URL(url)
                val baseUrl = "${urlObj.protocol}://${urlObj.host}/"
                val path = urlObj.path
                val voicesService =
                    Retrofit
                        .Builder()
                        .baseUrl(baseUrl)
                        .addCallAdapterFactory(callAdapterFactory)
                        .addConverterFactory(MoshiConverterFactory.create())
                        .client(okHttpClient)
                        .build()
                        .newBuilder()
                        .build()
                        .create(VoicesService::class.java)
                when (val voices = voicesService.getAvailableVoices(path)) {
                    is APIResult.NetworkError, is APIResult.Failure ->
                        liveData.postValue(
                            AudioAvailableVoicesApiState.Failure,
                        )
                    is APIResult.Success -> {
                        if (voices.data?.voices != null && voices.data.voices.isNotEmpty()) {
                            liveData.postValue(
                                AudioAvailableVoicesApiState.AvailableVoices(voices.data),
                            )
                        } else {
                            liveData.postValue(AudioAvailableVoicesApiState.Failure)
                        }
                    }
                }
            } catch (ex: Exception) {
                liveData.postValue(AudioAvailableVoicesApiState.Failure)
            }
            return liveData
        }
    }

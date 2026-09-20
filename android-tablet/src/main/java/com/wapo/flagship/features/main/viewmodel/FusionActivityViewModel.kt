package com.wapo.flagship.features.main.viewmodel

import com.wapo.android.commons.util.Logger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.Date
import com.wapo.flagship.features.articles2.repo.Articles2Repository
import com.wapo.flagship.features.articles2.tracking.AudioTrackerImpl
import com.wapo.flagship.features.articles2.utils.getUrlWithoutParameters
import com.wapo.flagship.features.articles2.utils.toTrackingInfo
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.toAudioAdConfig
import com.wapo.flagship.features.grid.AudioArticleVoiceType
import com.wapo.flagship.model.Status
import com.wapo.flagship.querypolicies.Query
import com.wapo.flagship.util.coroutines.DispatcherProvider
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.foryou.data.RecommendationsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FusionActivityViewModel
@Inject
constructor(
    val dispatcherProvider: DispatcherProvider,
    private val repository: Articles2Repository,
) : ViewModel() {
    var navigatingFromSectionsMenu: Boolean = false

    // currently active recommendation article being viewed
    val activeRecommendationArticle = MediatorLiveData<RecommendationsItem?>()

    /**
     * Flag used to keep track of when user clicks on search from MainActivity
     */
    var openedSearch = false

    /**
     * This is called when trackPageSelected to determine if we are sending analytics from search
     */
    fun searchStatus(pageName: String): Boolean {
        if (openedSearch) {
            Measurement.trackBackToSectionFromSearch(pageName)
            openedSearch = false
            return true
        }
        return false
    }

    fun fetchArticleAudioConfig(
        url: String,
        tabName: String?,
        appSection: String?,
        isActionAudio: Boolean,
        speed: Float,
        isAudioCarousel: Boolean,
        feed: String?,
        isFlexAudio: Boolean,
        isActionButton: Boolean,
    ): LiveData<AudioMediaConfig?> {
        val liveData = MediatorLiveData<AudioMediaConfig?>()
        val fetchData =
            repository.fetchData(Query(url), viewModelScope, dispatcherProvider.io)

        liveData.addSource(fetchData) { status ->
            var config: AudioMediaConfig? = null
            if (status is Status.Network) {
                config =
                    mapToAudioConfig(
                        status.data,
                        tabName,
                        appSection,
                        isActionAudio,
                        speed,
                        isAudioCarousel,
                        feed,
                        isFlexAudio,
                        isActionButton,
                    )
            } else if (status is Status.Cache) {
                config =
                    mapToAudioConfig(
                        status.data,
                        tabName,
                        appSection,
                        isActionAudio,
                        speed,
                        isAudioCarousel,
                        feed,
                        isFlexAudio,
                        isActionButton,
                    )
            }
            liveData.postValue(config)
        }
        return liveData
    }

    private fun mapToAudioConfig(
        article: Article2?,
        tabName: String?,
        appSection: String?,
        isActionAudio: Boolean,
        speed: Float,
        isAudioCarousel: Boolean,
        feed: String?,
        isFlexAudio: Boolean,
        isActionButton: Boolean,
    ): AudioMediaConfig? {
        val audio = article?.audio ?: return null
        return AudioMediaConfig(
            mediaId = audio.mediaId,
            manifestUrl = audio.manifestUrl,
            humanAdsUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
            humanRawUrl = if (audio.subtype == AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
            adsUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.adsUrl else null,
            rawUrl = if (audio.subtype != AudioArticleVoiceType.HUMAN.name.lowercase()) audio.rawUrl else null,
            titlePrefix = audio.title?.prefix,
            titleSeparator = audio.title?.separator,
            title = audio.title?.content ?: article.title,
            primaryLabel = audio.label?.displayLabel,
            secondaryLabel = audio.label?.displayTransparency,
            date = (article.items?.firstOrNull { item -> item is Date } as? Date)?.content,
            imageUrl = article.audio.image?.imageURL,
            imageCaption = article.audio.image?.fullCaption,
            contentUrl = getUrlWithoutParameters(article.contenturl),
            sectionName = article.section,
            caption = article.audio.caption,
            voices = null,
            arcId = article.arcId,
            audioTracking =
                AudioTrackerImpl(
                    tabName,
                    appSection,
                    article.omniture?.toTrackingInfo(),
                    isActionAudio,
                    speed,
                    isAudioCarousel,
                    feed,
                    isFlexAudio,
                    isActionButton,
                ),
            labelStyle = audio.label?.style,
            adConfig = audio.adConfig?.toAudioAdConfig(),
        )
    }

    fun fetchArticle(
        url: String
    ): LiveData<Article2?> {

        val liveData = MediatorLiveData<Article2?>()
        val fetchData =
            repository.fetchData(Query(url), viewModelScope, dispatcherProvider.io)

        liveData.addSource(fetchData) { status ->
            var article: Article2? = null
            when (status) {
                is Status.Network -> {
                    article = status.data
                }

                is Status.Cache -> {
                    article = status.data
                }

                is Status.Error -> {
                    Logger.e(
                        "FusionActivityViewModel",
                        "Error fetching article: ${status.message}"
                    )
                }

                is Status.Error415 -> {
                    Logger.e(
                        "FusionActivityViewModel",
                        "Error415 fetching article: ${status.article415}",
                    )
                }
            }
            liveData.postValue(article)
        }
        return liveData
    }
}

package com.wapo.flagship.features.video.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.adsinf.policy.AdService
import com.wapo.flagship.domain.repository.PrefUtilsRepo
import com.wapo.flagship.features.articles2.ads.targeting.TargetingContentUIState
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.posttv.VideoManager2
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.video.repo.VerticalVideoRepository
import com.wapo.flagship.features.wpvideos.data.WatchVideosApi
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Common ViewModel for Videos to perform any Videos related operations, manager operations,
 * listen for events and to query system level preferences.
 */
@HiltViewModel
class VideoActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contextualTargetingContentConfig: ContextualTargetingContent?,
    private val verticalVideoRepository: VerticalVideoRepository,
    val pageConfig: PageConfig,
    private val adService: AdService,
    private val prefUtilsRepo: PrefUtilsRepo
) : ViewModel() {
    /**
     * Event to be reported once Videos Warmup is done. Refer PostTvWarmUp classes.
     */
    private val _videoLoadedEvent = LiveEvent<Pair<String, String>>()
    val videoLoadedEvent: LiveData<Pair<String, String>> = _videoLoadedEvent

    private val _videoClickEvent = LiveEvent<String>()
    val videoClickEvent: LiveData<String> = _videoClickEvent

    private val _openVideoEvent = LiveEvent<Video>()
    val openVideoEvent: LiveData<Video> = _openVideoEvent

    var fetchId: String? = null
    private var offset: Int = 0

    private val _fetchNextVideo = MutableLiveData<WatchVideosApi>()
    val fetchNextVideo: LiveData<WatchVideosApi> = _fetchNextVideo

    private val _tooltipState = MutableStateFlow(false)
    val tooltipState: StateFlow<Boolean> = _tooltipState.asStateFlow()

    private val _targetingContentUiState: HashMap<String, MutableLiveData<TargetingContentUIState?>> =
        HashMap()
    val targetingContentUiState: Map<String, LiveData<TargetingContentUIState?>> =
        _targetingContentUiState

    private val _refreshVideosForAdsModeEvent = LiveEvent<Unit>()
    val refreshVideosForAdsModeEvent: LiveData<Unit> = _refreshVideosForAdsModeEvent

    init {
        observeAdsModeChanges()
    }

    /**
     * Sections and Articles are using [VideoManager2] class for any video operations and
     * it is initialized by the application class. It is safe to access it as a non null type.
     */
    fun getVideoManager2(): VideoManager2 =
        (context.applicationContext as? PostTvApplication)?.videoManager2!!

    fun canAutoPlayInlineVideo(): Boolean = AppPreferences.isAutoplayVideosOn()

    fun isAdsContentContextualTargetingEnabled(): Boolean {
        return contextualTargetingContentConfig?.enabled ?: false
    }

    fun dispatchVideosLoadedEvent(
        metaId: String,
        videoId: String,
    ) {
        _videoLoadedEvent.postValue(Pair(metaId, videoId))
    }

    fun dispatchVideoClickEvent(videoId: String?) {
        val id = videoId ?: return
        if (id.isNotEmpty()) {
            _videoClickEvent.postValue(id)
        }
    }

    fun updateAdsContentUiState(videoId: String, state: TargetingContentUIState?) {
        if (videoId.isEmpty()) return
        if (!_targetingContentUiState.containsKey(videoId)) return
        _targetingContentUiState[videoId]?.value = state
    }

    fun clearAdsContentUiState(videoId: String) {
        if (videoId.isEmpty()) return
        if (_targetingContentUiState.containsKey(videoId)) {
            _targetingContentUiState.remove(videoId)
        }
    }

    fun createAdsContentUiState(videoId: String) {
        if (videoId.isEmpty()) return
        if (!_targetingContentUiState.containsKey(videoId)) {
            _targetingContentUiState[videoId] = MutableLiveData()
        }
    }

    override fun onCleared() {
        _targetingContentUiState.clear()
    }

    fun openWatchVideoCard(video: Video) {
        _openVideoEvent.value = video
    }

    fun setOffset(offset: Int) {
        this.offset = offset
    }

    fun durationFetchNextVideo(fetchId: String?) {
        if (fetchId != this.fetchId) {
            this.fetchId = fetchId
            fetchVideo()
        }
    }

    fun fetchVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            val watchApi = verticalVideoRepository.getNextVideo(offset)
            watchApi?.let { api ->
                offset += api.items?.size ?: 0
                _fetchNextVideo.postValue(api)
            }
        }
    }

    fun shouldShowTooltip() {
        viewModelScope.launch {
            _tooltipState.emit(prefUtilsRepo.shouldShowVideoScreenTooltip())
        }
    }

    fun tooltipViewed() {
        viewModelScope.launch {
            prefUtilsRepo.setShowVideoScreenTooltipViewed()
        }
    }

    private fun observeAdsModeChanges() {
        viewModelScope.launch {
            var isFirstEmission = true
            adService.adsMode.collect {
                if (isFirstEmission) {
                    isFirstEmission = false
                    return@collect
                }
                _refreshVideosForAdsModeEvent.postValue(Unit)
            }
        }
    }
}

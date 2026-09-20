package com.wapo.flagship.features.wpvideos.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.wpvideos.data.WatchVideoMapper
import com.wapo.flagship.features.wpvideos.models.WatchVideosEvent
import com.wapo.flagship.features.wpvideos.repo.WatchVideosRepository
import com.wapo.flagship.features.wpvideos.state.WatchVideosUIState
import com.washingtonpost.android.config.domain.manager.ConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchVideosViewModel @Inject constructor(
    private val watchVideosRepository: WatchVideosRepository,
    private val adService: AdService,
) : ViewModel() {

    private val _watchVideosState = MutableStateFlow<WatchVideosUIState>(WatchVideosUIState.Loading)
    val watchVideosState: StateFlow<WatchVideosUIState> = _watchVideosState.asStateFlow()

    private val _watchVideosEvent: MutableSharedFlow<WatchVideosEvent> = MutableSharedFlow(replay = 0)
    val watchVideosEvent: SharedFlow<WatchVideosEvent> = _watchVideosEvent

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    init {
        viewModelScope.launch {
            adService.adsMode.collect {
                // When ad mode changes, we might want to refresh to ensure adTagUrls are correctly set/removed
                // For now, if they are already ad-free, we don't need to do much unless we want to purge existing list.
                // Given this is a simple grid, a clear and re-fetch is the cleanest way to ensure consistency.
                if (_videos.value.isNotEmpty()) {
                    clearVideos()
                    fetchVideos(0)
                }
            }
        }
    }

    fun fetchVideos(offset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = ConfigManager.getInstance().config.videosConfig
            _watchVideosState.value = WatchVideosUIState.Loading
            try {
                val watchVideosApi = watchVideosRepository.getWpVideosFeed(offset)
                watchVideosApi.items?.let { wpItem ->
                    _videos.update {
                        val list = WatchVideoMapper.getWpVideoItemList(
                            wpItem,
                            adService.currentAdsMode == AdsModel.Disabled,
                            config.mobileMaxBitRate,
                            config.tabletMaxBitRate
                        )
                        _watchVideosState.value = WatchVideosUIState.Success(list)
                        it + list
                    }
                }
            } catch (e: Exception) {
                _watchVideosState.value =
                    WatchVideosUIState.Error(e.message ?: "Unknown error occurred")
            }

            _watchVideosEvent.emit(WatchVideosEvent.WatchVideosReady)
        }
    }

    fun clearVideos() {
        _videos.value = emptyList()
    }
}
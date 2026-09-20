package com.wapo.flagship.features.wpvideos.state

import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.wpvideos.data.WatchVideosApi

sealed class WatchVideosUIState {
    object Loading : WatchVideosUIState()
    data class Success(val list: List<Video>) : WatchVideosUIState()
    data class Error(val message: String) : WatchVideosUIState()
}
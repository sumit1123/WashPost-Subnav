/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewmodels

import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * [com.washingtonpost.android.recirculation.carousel.viewholders.AudioStyleCarouselViewHolder]
 * observes this ViewModel data to update its UI.
 * App dispatches the data when there is a state update from audio module.
 */
@HiltViewModel
class CarouselAudioMediaActivityViewModel @Inject constructor() : ViewModel() {

    private val _nowPlayingAudioItem: MutableStateFlow<NowPlayingAudioItem?> = MutableStateFlow(null)
    val nowPlayingAudioItem: StateFlow<NowPlayingAudioItem?> = _nowPlayingAudioItem

    fun dispatchNowPlayingItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        _nowPlayingAudioItem.value = nowPlayingAudioItem
    }
}

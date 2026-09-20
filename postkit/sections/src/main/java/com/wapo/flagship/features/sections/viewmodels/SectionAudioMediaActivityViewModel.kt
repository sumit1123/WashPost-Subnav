/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.sections.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * [com.wapo.flagship.features.grid.views.carousel.CarouselAudioHolder]
 * observes this ViewModel data to update its UI.
 * App dispatches the data when there is a state update from audio module.
 */
@HiltViewModel
class SectionAudioMediaActivityViewModel @Inject constructor() : ViewModel() {

    private val _pageState = MutableLiveData<SectionAudioPageState>()
    val pageState: LiveData<SectionAudioPageState> = _pageState

    private val _nowPlayingAudioItem: MutableLiveData<NowPlayingAudioItem?> =
        MutableLiveData<NowPlayingAudioItem?>()
    val nowPlayingAudioItem: LiveData<NowPlayingAudioItem?> = _nowPlayingAudioItem

    fun dispatchPageState(playListId: String?, position: Int) {
        _pageState.value = SectionAudioPageState(playListId, position)
    }

    fun dispatchNowPlayingItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        _nowPlayingAudioItem.value = nowPlayingAudioItem
    }

    companion object {
        const val TAG = "SectionAudioMediaVM"
    }
}

/**
 * Class to maintain expanded player's page change event data.
 */
data class SectionAudioPageState(
    val playListId: String?,
    val position: Int
)

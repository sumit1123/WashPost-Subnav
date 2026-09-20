package com.wapo.flagship.features.articles2.interfaces

import androidx.lifecycle.LiveData
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState

/**
 * This interface has all of the functions that are invoked by individual article items either in viewholders or recycler adapter.
 */
interface ExternalEventsCoordinator {
    /**
     * Provides LiveData to observe Polly playback state.
     */
    fun provideAudioPlaybackStateLiveData(): LiveData<AudioPlaybackState>

    fun provideNowPlayingAudioItemLiveData(): LiveData<NowPlayingAudioItem?>
}

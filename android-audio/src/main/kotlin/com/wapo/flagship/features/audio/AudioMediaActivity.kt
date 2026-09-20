package com.wapo.flagship.features.audio

import com.wapo.flagship.features.audio.viewmodels.AudioMediaActivityViewModel
import com.wapo.flagship.features.audio.viewmodels.PlaylistActivityViewModel

/**
 * Interface to define abstract methods for the Activities or Fragments to access
 * [AudioMediaActivityViewModel] and [PlaylistActivityViewModel] methods in view holders
 */
interface AudioMediaActivity {

    fun getAudioManager(): ClassicAudioManager2
    fun getAudioMediaActivityViewModel(): AudioMediaActivityViewModel
    fun getAudioPlaylistActivityViewModel(): PlaylistActivityViewModel
}
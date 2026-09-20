/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.models

sealed interface AudioPlaybackState {
    object None : AudioPlaybackState

    object JSONSourceInitializing: AudioPlaybackState
    object JSONSourceInitialized: AudioPlaybackState
    object JSONSourceError: AudioPlaybackState

    object Stopped : AudioPlaybackState
    object Paused : AudioPlaybackState

    interface Playing : AudioPlaybackState {
        object PlayingContent: Playing
        object PlayingAd: Playing
    }

    object FastForwarding : AudioPlaybackState
    object Rewinding : AudioPlaybackState
    object Buffering : AudioPlaybackState
    class Error(val errorCode: Int?, val errorMessage: CharSequence?) : AudioPlaybackState
    object Connecting : AudioPlaybackState
    object SkipToPrevious : AudioPlaybackState
    object SkipToNext : AudioPlaybackState
    object SkipToQueueItem : AudioPlaybackState

    object Cleared : AudioPlaybackState

    object LaunchTTS : AudioPlaybackState
}
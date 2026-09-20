package com.wapo.flagship.features.posttv.model

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Buffering : PlaybackState()
    object Ready : PlaybackState()
    object ShowCountdownTimer : PlaybackState()
    object CancelCountdownTimer: PlaybackState()
    object Ended : PlaybackState()
    class Error(val error: Exception?) : PlaybackState()
    object TrackChanged : PlaybackState()
    class PositionDiscontinuity(val reason: Int) : PlaybackState()
    object Paused: PlaybackState()
    object Resumed: PlaybackState()
    class FetchDurationReached(val id: String): PlaybackState()
}
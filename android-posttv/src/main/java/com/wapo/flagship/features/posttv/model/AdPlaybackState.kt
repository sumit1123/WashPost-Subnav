package com.wapo.flagship.features.posttv.model

import com.google.ads.interactivemedia.v3.api.AdErrorEvent

sealed class AdPlaybackState {
    object NoAd : AdPlaybackState()
    object Loaded : AdPlaybackState()
    object ContentPauseRequested : AdPlaybackState()
    object Started : AdPlaybackState()
    object AdProgress : AdPlaybackState()
    object FirstQuartile : AdPlaybackState()
    object MidPoint : AdPlaybackState()
    object ThirdQuartile : AdPlaybackState()
    object Completed : AdPlaybackState()
    object ContentResumeRequested : AdPlaybackState()
    object AllAdsCompleted : AdPlaybackState()
    class Error(val errorEvent: AdErrorEvent): AdPlaybackState()
}
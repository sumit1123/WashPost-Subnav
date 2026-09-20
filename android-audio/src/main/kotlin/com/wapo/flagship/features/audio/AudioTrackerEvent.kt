/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio

sealed class AudioTrackerEvent {
    object Start : AudioTrackerEvent()
    object SkipForward : AudioTrackerEvent()
    object SkipBackward : AudioTrackerEvent()
    object Complete : AudioTrackerEvent()
    object Reset : AudioTrackerEvent()
    class NextPlay(val position: Int) : AudioTrackerEvent()
    class PreviousPlay(val position: Int) : AudioTrackerEvent()
    class UpdateVoice(val voice: String) : AudioTrackerEvent()
    class UpdateSpeed(val speed: Float) : AudioTrackerEvent()
    class UpdateProgressThreshold(val position: Int) : AudioTrackerEvent()
    class UpdateAudioType(val playerType: PlayerType): AudioTrackerEvent()
    class UpdateRollThrough(val rollThrough: Boolean) : AudioTrackerEvent()
    class UpdateDuration(val duration:Long) : AudioTrackerEvent()
    class UpdatePlayAd(val playAd: String) : AudioTrackerEvent()
}
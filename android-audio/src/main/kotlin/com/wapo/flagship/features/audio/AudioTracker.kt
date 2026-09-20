/* Copyright (c) 2021 The Washington Post. All rights reserved.*/

package com.wapo.flagship.features.audio

import androidx.media3.common.Player
import com.wapo.flagship.features.audio.playlist.AudioTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo

interface AudioTracker {
    fun onStart()
    fun onSkipForward()
    fun onSkipBackward()
    fun onComplete()
    fun setIsRollThrough(isRollThrough: Boolean = false)
    fun onNextPlay(position: Int)
    fun onPreviousPlay(position: Int)
    fun updateVoice(voice: String)
    fun updateSpeed(speed: Float)
    fun updateProgressThreshold(position: Int)
    fun updateAudioType(playerType: PlayerType)
    fun updateAudioFromActionBarFlag(flag: Boolean)
    fun updateDuration(duration:Long)
    fun reset()
    fun updateIsCarousal(flag: Boolean)
    fun stopTrackingProgress()
    fun startTrackingProgress(player: Player, onProgress: ((Int) -> Unit)? = null)
    fun startTrackingTtsProgress(player: Player)
    fun updatePlayAd(playAd: String)
    fun getAudioTrackingInfo(): AudioTrackingInfo?
    fun getPersoPodTrackingInfo(): Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?
    fun setPersoPodTrackingInfo(persoPodInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?)
}
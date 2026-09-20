// Copyright (c) 2021 The Washington Post. All rights reserved.

package com.wapo.flagship.features.articles2.tracking

import com.wapo.android.commons.util.Logger
import androidx.media3.common.Player
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.playlist.AudioTrackingInfo
import com.wapo.flagship.features.audio.playlist.toPlaylistAudioTrackingInfo
import com.wapo.flagship.features.personalizedpodcasts.model.PersoPodTrackingInfo
import com.wapo.flagship.json.TrackingInfo
import com.wapo.flagship.util.tracking.Events
import com.wapo.flagship.util.tracking.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioTrackerImpl(
    val tabName: String? = null,
    val appSection: String? = null,
    val trackingInfo: TrackingInfo? = null,
    var isActionAudio: Boolean = false,
    var speed: Float = 1f,
    var isAudioCarousel: Boolean = false,
    val feed: String? = null,
    val isFlexAudio: Boolean = false,
    val isActionButton: Boolean = false,
    var isAudioPlaylist: Boolean = false,
    var isRecommended: Boolean = false,
    var duration: Long = 0L,
    val avArcId: String? = null,
    val avName: String? = null,
    var playAd: String? = null,
    var isAuto: Boolean = false,
    private var persoPodTrackingInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>? = null,
) : AudioTracker {
    private var isRollThrough: Boolean = false
    private var isSpeechStarted: Boolean = false
    private var isSpeechCompleted: Boolean = false
    private var isAudioFromActionBar: Boolean = false
    private val pathToView
        get() =
            when {
                isAuto && !isRollThrough -> Measurement.NAVIGATION_AUDIO_AUTO
                isAuto && isRollThrough -> Measurement.NAVIGATION_AUDIO_AUTO_ROLLTHROUGH
                audioType == Measurement.AUDIO_TYPE_STANDALONE -> Measurement.PATH_TO_VIEW_AUDIO_STANDALONE
                audioType == Measurement.AUDIO_TYPE_PODCAST -> Measurement.AUDIO_TYPE_PODCAST
                audioType == Measurement.PERSO_PODCAST -> Measurement.NAVIGATION_AUDIO_AUTO
                isRecommended && !isRollThrough -> Measurement.NAVIGATION_AUDIO_RECOMMENDATION
                isRecommended && isRollThrough -> Measurement.NAVIGATION_AUDIO_RECOMMENDATION_ROLLTHROUGH
                isAudioPlaylist && !isRollThrough -> Measurement.PATH_TO_VIEW_AUDIO_PLAYLIST
                isAudioPlaylist && isRollThrough -> Measurement.NAVIGATION_AUDIO_PLAYLIST_ROLL_THROUGH
                isFlexAudio -> Measurement.PATH_TO_VIEW_AUDIO_FLEX
                isActionButton -> Measurement.PATH_TO_VIEW_AUDIO_ACTION_BUTTON
                isAudioFromActionBar || isActionAudio -> Measurement.PATH_TO_VIEW_AUDIO_TOP_BAR
                isAudioCarousel && isRollThrough -> Measurement.NAVIGATION_AUDIO_CAROUSEL_ROLL_THROUGH
                isAudioCarousel && !isRollThrough -> Measurement.PATH_TO_VIEW_AUDIO_CAROUSEL
                else -> Measurement.PATH_TO_VIEW_AUDIO_INLINE
            }
    private var audioType: String? = null
    private var voice: String? = null

    override fun setIsRollThrough(isRollThrough: Boolean) {
        this.isRollThrough = isRollThrough
    }

    override fun onStart() {
        val event = mapEventByAudioType(Events.EVENT_SPEECH_START)
        event ?: return
        if (!isSpeechStarted) {
            trackingInfo?.let {
                Logger.d("TAG", "onStart tracking: $trackingInfo")
                Measurement.trackSpeechEvent(
                    trackingInfo,
                    tabName,
                    appSection,
                    pathToView,
                    audioType,
                    event,
                    speed,
                    voice,
                    feed,
                    duration,
                    avArcId,
                    avName,
                    playAd,
                )
                isSpeechStarted = true
            }
        }
    }

    override fun onSkipForward() {
        val event = mapEventByAudioType(Events.EVENT_SPEECH_NEXT)
        event ?: return
        trackingInfo?.let {
            Measurement.trackSpeechEvent(
                trackingInfo,
                tabName,
                appSection,
                pathToView,
                audioType,
                event,
                speed,
                voice,
                feed,
                duration,
                avArcId,
                avName,
                null,
            )
        }
    }

    override fun onSkipBackward() {
        val event = mapEventByAudioType(Events.EVENT_SPEECH_PREV)
        event ?: return
        trackingInfo?.let {
            Measurement.trackSpeechEvent(
                trackingInfo,
                tabName,
                appSection,
                pathToView,
                audioType,
                event,
                speed,
                voice,
                feed,
                duration,
                avArcId,
                avName,
                null,
            )
        }
    }

    override fun onComplete() {
        if (!isSpeechCompleted) {
            val event = mapEventByAudioType(Events.EVENT_SPEECH_COMPLETE)
            if (event != null) {
                trackingInfo?.let {
                    Measurement.trackSpeechEvent(
                        trackingInfo,
                        tabName,
                        appSection,
                        pathToView,
                        audioType,
                        event,
                        speed,
                        voice,
                        feed,
                        duration,
                        avArcId,
                        avName,
                        playAd,
                    )
                    isSpeechCompleted = true
                }
            }

            val progressEvent = mapEventByAudioType(Events.EVENT_SPEECH_PROGRESS, 100)
            if (progressEvent != null) {
                Measurement.trackSpeechProgress(
                    progressEvent,
                    trackingInfo,
                    tabName,
                    appSection,
                    pathToView,
                    audioType,
                    speed,
                    voice,
                    100,
                    duration,
                    avArcId,
                    avName,
                )
            }
        }
    }

    override fun onNextPlay(position: Int) {
        trackingInfo?.let {
            Measurement.setProgressThreshold(Measurement.getDefaultMap(), position + 1)
            // If roll through doesn't happen automatically, it will trigger through here and we do not
            // want to trigger the interaction event below.
            if (!isRollThrough) {
                Measurement.trackAudioPlayerNextPrevious(NEXT, position + 1, feed)
            }
        }
    }

    override fun onPreviousPlay(position: Int) {
        trackingInfo?.let {
            Measurement.setProgressThreshold(Measurement.getDefaultMap(), position + 1)
            Measurement.trackAudioPlayerNextPrevious(PREVIOUS, position + 1, feed)
        }
    }

    override fun updateVoice(voice: String) {
        this.voice = voice
    }

    override fun updateSpeed(speed: Float) {
        this.speed = speed
    }

    override fun updateAudioType(playerType: PlayerType) {
        audioType =
            when (playerType) {
                PlayerType.HUMAN -> {
                    Measurement.AUDIO_TYPE_HUMAN
                }
                PlayerType.STANDALONE -> {
                    Measurement.AUDIO_TYPE_STANDALONE
                }
                PlayerType.PODCAST -> {
                    Measurement.AUDIO_TYPE_PODCAST
                }
                PlayerType.PERSO_PODCAST -> {
                    Measurement.PERSO_PODCAST
                }
                else -> {
                    Measurement.AUDIO_TYPE_POLLY
                }
            }
    }

    override fun updateAudioFromActionBarFlag(flag: Boolean) {
        this.isAudioFromActionBar = flag
    }

    override fun updateProgressThreshold(position: Int) {
        trackingInfo?.let {
            Measurement.setProgressThreshold(Measurement.getDefaultMap(), position + 1)
        }
    }

    override fun updateIsCarousal(flag: Boolean) {
        isAudioPlaylist = flag
        isAudioCarousel = !flag
    }

    override fun updateDuration(duration: Long) {
        this.duration = duration
    }

    override fun updatePlayAd(playAd: String) {
        this.playAd = playAd
    }

    override fun reset() {
        isSpeechStarted = false
        isSpeechCompleted = false
        isRollThrough = false
    }

    override fun getAudioTrackingInfo(): AudioTrackingInfo? {
        return trackingInfo?.toPlaylistAudioTrackingInfo()
    }

    override fun getPersoPodTrackingInfo(): Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>? {
        return persoPodTrackingInfo
    }

    override fun setPersoPodTrackingInfo(persoPodInfo: Pair<PersoPodTrackingInfo?, PersoPodTrackingInfo?>?) {
        persoPodTrackingInfo = persoPodInfo
    }

    /**
     * Audio progress flow that emits the current percent progress of the player.
     * - If user skips audio it will emit all the intermediate increments of progress
     *   ie. 5 skipped to 30, it will emit 5, 10, 15, 20, 25, 30.
     */
    private fun audioProgress(
        player: Player,
        increment: Int = 5,
    ) = flow {
        var lastPercent = 0
        while (true) {
            val percentProgress = player.percentProgress()
            val delta = percentProgress - lastPercent
            if (percentProgress > lastPercent && delta >= increment) {
                val start = lastPercent - lastPercent % increment + increment
                for (i in start..percentProgress step increment) {
                    emit(i)
                }
                lastPercent = percentProgress
            }
            withContext(Dispatchers.IO) {
                delay(1000)
            }
            Logger.d("FlowsRunning", "running")
        }
    }

    private var repeatJob: Job? = null
    private var onProgressed: ((Int) -> Unit)? = null

    override fun startTrackingTtsProgress(player: Player) {
        onProgressed = {
            val event = mapEventByAudioType(Events.EVENT_SPEECH_PROGRESS, it)
            if (event != null) {
                Measurement.trackSpeechProgress(
                    event,
                    trackingInfo,
                    tabName,
                    appSection,
                    pathToView,
                    audioType,
                    speed,
                    voice,
                    it,
                    duration,
                    avArcId,
                    avName,
                )
            }
        }
        startTrackingProgress(player, onProgressed)
    }

    override fun startTrackingProgress(
        player: Player,
        onProgress: ((Int) -> Unit)?,
    ) {
        onProgressed = onProgress
        repeatJob =
            MainScope().launch {
                audioProgress(player).cancellable().distinctUntilChanged().collect {
                    onProgressed?.invoke(it)
                }
            }
    }

    override fun stopTrackingProgress() {
        repeatJob?.cancel()
        repeatJob = null
        onProgressed = null
    }

    private fun Player.percentProgress(): Int =
        if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat() * 100).toInt()
        } else {
            0
        }

    private fun mapEventByAudioType(
        sourceEvent: Events,
        duration: Int? = null,
    ): Events? =
        when (audioType) {
            Measurement.AUDIO_TYPE_STANDALONE -> {
                val event: Events? =
                    when (sourceEvent) {
                        Events.EVENT_SPEECH_START -> Events.EVENT_AUDIO_START
                        Events.EVENT_SPEECH_PROGRESS -> {
                            if (duration == 25) {
                                Events.EVENT_AUDIO_PLAYED_25
                            } else if (duration == 50) {
                                Events.EVENT_AUDIO_PLAYED_50
                            } else if (duration == 75) {
                                Events.EVENT_AUDIO_PLAYED_75
                            } else {
                                null
                            }
                        }
                        Events.EVENT_SPEECH_COMPLETE -> Events.EVENT_AUDIO_COMPLETE

                        else -> null
                    }
                event
            }

            else -> {
                sourceEvent
            }
        }

    companion object {
        private val PREVIOUS = "audio_carousel_play_previous"
        private val NEXT = "audio_carousel_play_next"
    }
}

package com.wapo.flagship.features.audio.config

import androidx.media3.common.Player
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.models.MediaItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastTracker(
    val listener: AudioProvider,
    val mediaItemData: MediaItemData?,
    val audioTracker: AudioTracker?
) {
    var player: Player? = null
    var progressThreshold: Int = 0

    private var highestPercent: Double = 0.0

    // This job is used to continue repeating tracking every 5 seconds
    private var repeatJob: Job? = null

    // This job is used to start tracking after 1 second of start of audio play
    private var startJob: Job? = null

    /*
        Starts tracking jobs
     */
    fun startTracking() {
        startJob = GlobalScope.launch(Dispatchers.IO) {
            delay(START_DELAY_IN_MILLISECONDS)
            withContext(Dispatchers.Main) {
                trackProgress()
            }
        }

        repeatJob = GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(REPEAT_DELAY_IN_MILLISECONDS)
                withContext(Dispatchers.Main) {
                    trackProgress()
                }
            }
        }

        player?.let {
            audioTracker?.startTrackingProgress(it) { percent ->
                trackProgressIncrement(percent, listener, it.duration)
            }
        }
    }

    fun trackRollThroughComplete(duration: Long?) {
        trackPercent(100, listener, duration)
        trackProgressIncrement(100, listener, duration)
    }

    /*
        Function that actually tracks the progress of the podcast playback
     */
    private fun trackProgress() {
        try {
            // requirements:
            // seeks backwards = no analytics
            // seek forward = fire all events which were passed by
            val durationDouble = player?.duration
            val currentPosition = player?.currentPosition?.toDouble()
            if (durationDouble != null && currentPosition != null) {
                val currentPercent = (currentPosition / durationDouble) * 100

                if (currentPercent > 0 && highestPercent == 0.0) {
                    listener.onPodcastEvent(
                        AudioProvider.EventType.ON_PLAY_STARTED,
                        mediaItemData,
                        null,
                        audioTracker,
                        durationDouble,
                        progressThreshold
                    )
                }

                if (currentPercent >= 25 && highestPercent < 25) {
                    trackPercent(25, listener, durationDouble)
                }
                if (currentPercent >= 50 && highestPercent < 50) {
                    trackPercent(50, listener, durationDouble)
                }
                if (currentPercent >= 75 && highestPercent < 75) {
                    trackPercent(75, listener, durationDouble)
                }
                if (player?.playbackState == Player.STATE_ENDED) {
                    trackPercent(100, listener, durationDouble)
                    trackProgressIncrement(100, listener, durationDouble)
                }
                highestPercent = when {
                    currentPercent > highestPercent -> currentPercent
                    else -> highestPercent
                }
            } else {
                Logger.d(TAG, "ExoPlayer error")
            }
        } catch (e: Exception) {
            Logger.d(TAG, "Tracking error", e)
        }
    }

    fun cancelJobs() {
        audioTracker?.stopTrackingProgress()
        startJob?.cancel()
        repeatJob?.cancel()
        repeatJob = null
        startJob = null
        // finalize by tracking current progress (e.g. they scrubbed straight to the end)
        trackProgress()
    }

    private fun trackPercent(percent: Byte, listener: AudioProvider? = null, duration: Long?) {
        listener?.onPodcastEvent(
            AudioProvider.EventType.ON_PERCENTAGE_PLAYED,
            mediaItemData,
            percent,
            audioTracker,
            duration,
            progressThreshold
        )
    }

    private fun trackProgressIncrement(percent:Int, listener: AudioProvider? = null, duration: Long?) {
        listener?.onPodcastEvent(
            AudioProvider.EventType.ON_PROGRESS,
            mediaItemData,
            percent,
            audioTracker,
            duration,
            progressThreshold
        )
    }


    companion object {
        private val TAG: String = PodcastTracker::class.java.simpleName
        private const val START_DELAY_IN_MILLISECONDS = 1000L
        private const val REPEAT_DELAY_IN_MILLISECONDS = 5000L
    }
}
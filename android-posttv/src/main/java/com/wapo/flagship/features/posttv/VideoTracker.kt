package com.wapo.flagship.features.posttv

import android.os.Handler
import android.os.Looper
import com.wapo.android.commons.util.Logger
import androidx.media3.exoplayer.ExoPlayer
import com.wapo.flagship.features.posttv.model.TrackingType
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class VideoTracker private constructor(private val listener: VideoEventListener?) {
    private lateinit var exoPlayer: WeakReference<ExoPlayer>
    private lateinit var handler: WeakReference<Handler>
    private var highestPercent: Double = 0.0
    private val observable: Observable<Unit> = Observable.fromCallable {
        trackProgress()
    }
            .repeatWhen { o ->
                o.concatMap {
                    Observable.timer(REPEAT_DELAY_IN_SECONDS, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())

                }
            }
            .doOnUnsubscribe {
                // finalize by tracking current progress (e.g. they scrubbed straight to the end)
                trackProgress()
                onVideoEvent(TrackingType.ON_PLAY_STOPPED)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    private fun trackProgress() {
        handler?.get() ?: Handler(Looper.getMainLooper()).post {
            try {
                // requirements:
                // seeks backwards = no analytics
                // seek forward = fire all events which were passed by
                // if we ever add multiple tracks, I think we should use MediaSource setTag
                val isPlayingAd = exoPlayer.get()?.isPlayingAd
                if (isPlayingAd == false) {
                    val duration = exoPlayer.get()?.duration?.toDouble()
                    val currentPosition = exoPlayer.get()?.currentPosition?.toDouble()
                    if (duration != null && currentPosition != null) {
                        val currentPercent = (currentPosition / duration) * 100
                        if (currentPercent >= 0 && highestPercent == 0.0) {
                            onVideoEvent(TrackingType.ON_PLAY_STARTED)
                        }
                        if (currentPercent >= 25 && highestPercent < 25) {
                            onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 25)
                        }
                        if (currentPercent >= 50 && highestPercent < 50) {
                            onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 50)
                        }
                        if (currentPercent >= 75 && highestPercent < 75) {
                            onVideoEvent(TrackingType.VIDEO_PERCENTAGE_WATCHED, 75)
                        }
                        highestPercent = when {
                            currentPercent > highestPercent -> currentPercent
                            else -> highestPercent
                        }
                    } else {
                        Logger.d(TAG, "ExoPlayer error")
                    }
                }
            } catch (e: Exception) {
                Logger.d(TAG, "Tracking error", e)
            }
        }
    }

    @Synchronized
    private fun onVideoEvent(trackingType: TrackingType, value: Any? = null) {
        listener?.onVideoEvent(trackingType, value)
    }

    fun getObs(): Observable<Unit> {
        return observable
    }

    private fun setExoPlayer(exoPlayer: ExoPlayer) {
        this.exoPlayer = WeakReference(exoPlayer)
        highestPercent = 0.0
        handler = WeakReference(Handler(exoPlayer.applicationLooper))
    }

    interface VideoEventListener {
        fun onVideoEvent(trackingType: TrackingType, value: Any? = null)
    }

    companion object {
        @Volatile
        private var INSTANCE: VideoTracker? = null
        private val TAG: String = VideoTracker::class.java.simpleName
        private const val REPEAT_DELAY_IN_SECONDS = 5L

        @JvmStatic
        fun getInstance(listener: VideoEventListener?, exoPlayer: ExoPlayer): VideoTracker =
            INSTANCE?.also {
                it.setExoPlayer(exoPlayer)
            } ?: synchronized(this) {
                INSTANCE?.also {
                    it.setExoPlayer(exoPlayer)
                } ?: VideoTracker(listener).also {
                    INSTANCE = it
                    it.setExoPlayer(exoPlayer)
                }
            }
    }
}
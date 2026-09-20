// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.video

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.posttv.FullScreenPlayerFragment
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PiPActivity
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.Share
import com.wapo.flagship.util.UIUtil
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityFullScreenVideoBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Class to handle PiP mode for videos. It owns a player from [PostTvPlayer2Coordinator]
 * if one already exists, otherwise creates a new player.
 */
@AndroidEntryPoint
class FullScreenVideoActivity :
    BaseActivity(),
    PostTvActivity,
    PiPActivity {
    val tag = FullScreenVideoActivity::class.java.simpleName

    enum class LaunchMode {
        FULLSCREEN,
        PIP,
    }

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityFullScreenVideoBinding.inflate(layoutInflater)
    }

    private lateinit var parcel: FullScreenVideoParcel
    private var playerManager: PostTvPlayer2Manager? = null
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        processIntent()
        observePlaybackState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent()
    }

    override fun onDestroy() {
        releaseReceiver()
        super.onDestroy()
    }

    private fun processIntent() {
        parcel = FullScreenVideoParcel(intent)
        if (parcel.getLaunchMode() == LaunchMode.PIP) {
            if (!UIUtil.isPIPSupported()) finish()
            enterPiPMode()
        }
        val video: Video =
            parcel.getVideo() ?: throw IllegalArgumentException(
                "video object is null",
            )
        val playerName = parcel.getPlayerName()
        if (playerName.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "playerName is empty or null",
            )
        }
        playerManager =
            if (PostTvPlayer2Coordinator.doesPlayerExist(playerName)) {
                PostTvPlayer2Coordinator.getPlayer(playerName).also {
                    it.updateOwnerActivity(this)
                    it.resumeMedia()
                }
            } else {
                PostTvPlayer2Coordinator.getOrCreatePlayer(playerName, this).also {
                    it.playMedia(video)
                }
            }
        playerManager?.hideControllerOptions(com.wapo.flagship.features.posttv.R.id.exo_fullscreen)
        attachFragment(playerName)
        hideSystemBars()
    }

    private fun attachFragment(playerName: String) {
        val fragment = FullScreenPlayerFragment().apply { setPlayerName(playerName) }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.full_screen_video_container, fragment, FRAGMENT_TAG)
            .commit()
    }

    @SuppressLint("NewApi")
    override fun enterPiPMode() {
        val paramsBuilder = PictureInPictureParams.Builder()
        val aspectRatio = parcel.getVideo()?.aspectRatio ?: 0f
        if (aspectRatio > 0f) {
            // Aspect ratio must be between 2.39:1 and 1:2.39 (inclusive)
            // https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setAspectRatio(android.util.Rational)
            val pipAspectRatio =
                if (aspectRatio > 2.39f) {
                    2.39f
                } else if (aspectRatio < 0.418410) {
                    0.418410f
                } else {
                    aspectRatio
                }
            paramsBuilder
                .setAspectRatio(Rational(1000000, ((1f / pipAspectRatio) * 1000000).toInt()))
        }
        enterPictureInPictureMode(paramsBuilder.build())
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerManager?.isPlaying() == true) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            // might clicked on the close button
            releaseReceiver()
            finishAndRemoveTask()
            return
        }
        if (isInPictureInPictureMode) {
            if (playerManager?.isPlaying() == true) {
                updatePauseAction()
            } else {
                updatePlayAction()
            }

            mReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        if (VideoActivity.ACTION_MEDIA_CONTROL != intent.action) {
                            return
                        }
                        when (intent.getIntExtra(VideoActivity.EXTRA_CONTROL_TYPE, 0)) {
                            CONTROL_TYPE_PLAY -> {
                                playerManager?.resetPlayerIfEnded()
                                playerManager?.resumeMedia()
                                updatePauseAction()
                            }
                            CONTROL_TYPE_PAUSE -> {
                                playerManager?.pauseMedia()
                                updatePlayAction()
                            }
                        }
                    }
                }
            ContextCompat.registerReceiver(
                this,
                mReceiver,
                IntentFilter(VideoActivity.ACTION_MEDIA_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            playerManager?.useController(false)
        } else {
            releaseReceiver()
            playerManager?.useController(true)
        }
        hideSystemBars()
    }

    private fun updatePiPActions(
        @DrawableRes iconId: Int,
        title: String?,
        controlType: Int,
        requestCode: Int,
    ) {
        val actions = ArrayList<RemoteAction>()
        val intent =
            PendingIntent.getBroadcast(
                this,
                requestCode,
                Intent(VideoActivity.ACTION_MEDIA_CONTROL).putExtra(
                    VideoActivity.EXTRA_CONTROL_TYPE,
                    controlType,
                ),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val icon: Icon = Icon.createWithResource(this, iconId)
        actions.add(RemoteAction(icon, title ?: "", title ?: "", intent))
        val params = PictureInPictureParams.Builder().setActions(actions)
        setPictureInPictureParams(params.build())
    }

    private fun updatePlayAction() {
        updatePiPActions(R.drawable.gallery_play, PLAY_TITLE, CONTROL_TYPE_PLAY, REQUEST_PLAY)
    }

    private fun updatePauseAction() {
        updatePiPActions(R.drawable.gallery_pause, PAUSE_TITLE, CONTROL_TYPE_PAUSE, REQUEST_PAUSE)
    }

    private fun hideSystemBars() {
        val window = window ?: return
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun releaseReceiver() {
        if (mReceiver != null) unregisterReceiver(mReceiver)
        mReceiver = null
    }

    override fun shareVideo(
        headline: String?,
        shareUrl: String?,
    ) {
        if (shareUrl == null) {
            Toast.makeText(this, "Something went wrong, try again later", Toast.LENGTH_SHORT).show()
            return
        }
        Share
            .Builder()
            .shareUrl(shareUrl)
            .headline(headline)
            .fromPush(false)
            .arcId("")
            .build()
            .shareItem(this)
    }

    private fun observePlaybackState() {
        playerManager?.playbackState?.observe(this) {
            when (it) {
                PlaybackState.Ended -> {
                    updatePlayAction()
                }
                else -> {
                }
            }
        }
    }

    override fun startPIP(mVideo: Video?) {
    }

    override fun startPIP(
        video: Video?,
        playerName: String?,
    ) {
        enterPiPMode()
    }

    override fun startFullScreen(video: Video?, playerName: String) {

    }

    override fun isPIPEnabled(): Boolean = PrefUtils.getPIPEnabled(this)

    @Deprecated("Deprecated in Java")
    override fun onTrackingEvent(
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
    }

    override fun onTrackingEvent(
        videoType: VideoTracker2.VideoType,
        type: TrackingType,
        video: Video,
        value: Any?,
    ) {
    }

    override fun openWeb(url: String?) {
    }

    override fun logVideoError(eventLogBuilder: EventLog.Builder?) {
        // log non n/w errors
        val isNetworkError = !isConnectedOrConnecting(applicationContext)
        if (eventLogBuilder != null && !isNetworkError) {
            RemoteLog.e(applicationContext, eventLogBuilder.build())
        }
    }

    companion object {
        const val FRAGMENT_TAG = "FULL_SCREEN_FRAGMENT"
        const val FULL_SCREEN_PLAYER = "FullScreenVideoActivity"
        const val CONTROL_TYPE_PLAY = 1
        const val CONTROL_TYPE_PAUSE = 2
        const val REQUEST_PLAY = 1
        const val REQUEST_PAUSE = 2
        const val PLAY_TITLE = "play"
        const val PAUSE_TITLE = "pause"
    }
}

// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship

import android.os.Bundle
import com.wapo.android.commons.util.Logger
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.wapo.android.commons.logs.EventLog
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.listeners.PostTvActivity
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.TrackingType
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.posttv.util.Player2ViewModel
import com.wapo.flagship.util.Share
import com.washingtonpost.android.databinding.ActivityTestPostTvPlayer2Binding

/**
 * Test activity class to verify [PostTvPlayer2Manager] and related classes.
 */
class TestPostTvPlayer2Activity :
    AppCompatActivity(),
    AdapterView.OnItemSelectedListener,
    PostTvActivity {
    val tag = TestPostTvPlayer2Activity::class.java.simpleName

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityTestPostTvPlayer2Binding.inflate(layoutInflater)
    }

    private val player2ViewModel: Player2ViewModel by viewModels()
    private lateinit var player2Manager: PostTvPlayer2Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        // Hide the status bar.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        prepareActionsSpinner()

        player2Manager = PostTvPlayer2Coordinator.getOrCreatePlayer(tag, this)
        player2Manager.apply {
            // Default controller view (R.layout.posttv2_exo_player_view) will be used if one is not
            // provided here.
            // updatePlayerContainerView(R.layout.test_post_tv_player2_player_view)
            updateViewModel(player2ViewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        observePlayerState()
        viewBinding.actionsSpinner.setSelection(0)
    }

    private fun prepareActionsSpinner() {
        val actions =
            arrayOf(
                "Drop Down",
                "Video1",
                "Video2",
                "Video3",
                "Immersive",
                "Exit Immersive",
            )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)
        viewBinding.actionsSpinner.apply {
            setAdapter(adapter)
            setSelection(0, false)
            onItemSelectedListener = this@TestPostTvPlayer2Activity
        }
    }

    private fun observePlayerState() {
        player2Manager.playbackState.observe(this) { state ->
            Logger.d(tag, "observePlayerState, state=$state")
            when (state) {
                is PlaybackState.Idle -> {
                }
                is PlaybackState.Buffering -> {
                }
                is PlaybackState.Ready -> {
                }
                is PlaybackState.Ended -> {
                }
                is PlaybackState.Error -> {
                }
                else -> {
                    // no op
                }
            }
        }
    }

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long,
    ) {
        Logger.d(tag, "onItemSelected, position=$position, this=${this.hashCode()}")
        when (position) {
            1 -> {
                // val url = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
                val url = "https://d21rhj7n383afu.cloudfront.net/washpost-production/The_Washington_Post/20220621/62b235e45dbed118da9094e3/62b235ed00e1586bfbaf9cec/mobile.m3u8"
                player2Manager.addPlayerContainerViewToItemView(viewBinding.mediaContainer)
                player2Manager.playMedia(getVideoObject(url))
            }
            2 -> {
                val url = "https://d21rhj7n383afu.cloudfront.net/washpost-production/The_Washington_Post/20220419/625eb9115bca752cab0481b7/6260263b57a5914188654304/mobile.m3u8"
                player2Manager.addPlayerContainerViewToItemView(viewBinding.mediaContainer)
                player2Manager.playMedia(getVideoObject(url))
            }
            3 -> {
                val url = "https://d21rhj7n383afu.cloudfront.net/washpost-production/The_Washington_Post/20220413/62572c8ed0bbdf45714cd241/62572c9652faff001134bbfb/mobile.m3u8"
                player2Manager.addPlayerContainerViewToItemView(viewBinding.mediaContainer)
                player2Manager.playMedia(getVideoObject(url))
            }
            4 -> {
                hideSystemUi()
            }
            5 -> {
                showSystemUi()
            }
            else -> {
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
    }

    private fun showSystemUi() {
        window.decorView.systemUiVisibility = 0
    }

    private fun getVideoObject(url: String): Video =
        Video
            .Builder()
            .setId(url)
            .setContentUrl(url)
            .setShareUrl(url)
            .setContentId(url)
            .setFallbackUrl(url)
            .build()

    override fun addFragment(
        viewID: Int,
        fragment: Fragment,
        shouldSaveState: Boolean,
    ) {
    }

    override fun removeFragment(
        fragment: Fragment?,
        shouldSaveState: Boolean,
    ) {
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
            .isVideoShare(true)
            .build()
            .shareItem(this)
    }

    override fun startPIP(mVideo: Video?) {
    }

    override fun startPIP(
        video: Video?,
        playerName: String?,
    ) {
    }

    override fun startFullScreen(video: Video?, playerName: String?) {
    }

    override fun isPIPEnabled(): Boolean = false

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

    override fun onVideoStarted() {
    }

    override fun openWeb(url: String?) {
    }

    override fun logVideoError(eventLogBuilder: EventLog.Builder?) {
    }
}

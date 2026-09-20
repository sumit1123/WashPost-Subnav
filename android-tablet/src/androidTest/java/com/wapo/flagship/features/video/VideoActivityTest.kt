// Copyright (c) 2019 The Washington Post. All rights reserved.

package com.wapo.flagship.features.video

import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.View
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.wapo.flagship.wapomain.MainActivity
import com.washingtonpost.android.R
import junit.framework.Assert.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoActivityTest {
    private val mainActivityActivityTestRule =
        ActivityTestRule<MainActivity>(
            MainActivity::class.java,
        )
    private val rule = ActivityTestRule<VideoActivity>(VideoActivity::class.java)
    private lateinit var idlingResource: ExoPlayerIdlingResource
    private lateinit var player: Player

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getTargetContext()
        val videoIntent =
            VideoActivity.createIntent(
                context,
                VideoActivity::class.java,
                "https://d21rhj7n383afu.cloudfront.net/washpost-production/The_Washington_Post/20190718/5d30b8c946e0fb00094408d5/5d30c022cff47e00098383bc_1439412153584-wn5qra_t_1563476006079_640_360_600.mp4",
                null,
                "https://www.washingtonpost.com/video/politics/trump-falsely-says-he-stopped-send-her-back-chants/2019/07/18/35474a2b-072b-4133-9351-eb307eea482d_video.html",
                "Trump falsely says he stopped ‘send her back’ chants",
                null,
                null,
                "https://www.washingtonpost.com/video/politics/trump-falsely-says-he-stopped-send-her-back-chants/2019/07/18/35474a2b-072b-4133-9351-eb307eea482d_video.html",
                "Trump falsely says he stopped ‘send her back’ chants",
                null,
                null,
                null,
            )
        mainActivityActivityTestRule.launchActivity(Intent(context, MainActivity::class.java))
        rule.launchActivity(videoIntent)

        player = rule.activity.findViewById<PlayerView>(com.wapo.flagship.features.posttv.R.id.wapo_player_view).player!!
        idlingResource = ExoPlayerIdlingResource(player)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        rule.runOnUiThread {
            try {
                IdlingRegistry.getInstance().unregister(idlingResource)
                idlingResource.unregister()
            } catch (_: Throwable) {
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun movie_playingOnPip() {
        onView(withId(com.wapo.flagship.features.posttv.R.id.wapo_player_view))
            .perform(showControls())
//        // Click on the button to enter Picture-in-Picture mode
        onView(withId(com.wapo.flagship.features.posttv.R.id.exo_pip)).perform(click())

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // The Activity is paused. We cannot use Espresso to test paused activities.
        rule.runOnUiThread {
            // We are now in Picture-in-Picture mode
            val view: PlayerView = rule.activity.findViewById(com.wapo.flagship.features.posttv.R.id.wapo_player_view)
            assertTrue(rule.activity.isInPictureInPictureMode)
            assertTrue(
                view.player?.playbackState == Player.STATE_READY && view.player?.playWhenReady ?: false,
            )

            IdlingRegistry.getInstance().unregister(idlingResource)
            idlingResource.unregister()
        }
    }

    @Test
    @Ignore("Flaky test")
    @Throws(Throwable::class)
    fun movie_pauseAndResume() {
        // The movie should be playing on start
        onView(ViewMatchers.withId(com.wapo.flagship.features.posttv.R.id.wapo_player_view))
            .perform(showControls())
        // Pause
        // onView(withId(R.id.exo_pause)).perform(click())
        onView(withId(com.wapo.flagship.features.posttv.R.id.wapo_player_view)).check(matches((not(isPlaying()))))
        // Resume
        // onView(withId(R.id.exo_play)).perform(click())
        onView(withId(com.wapo.flagship.features.posttv.R.id.wapo_player_view)).check(matches(isPlaying()))
        // Closed captions
        onView(withId(com.wapo.flagship.features.posttv.R.id.exo_cc)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // The Activity is paused. We cannot use Espresso to test paused activities.
        rule.runOnUiThread {
            // val view: SubtitleView = rule.activity.findViewById(R.id.exo_subtitles)
            // assertNotNull(view)
            // assertTrue(view.visibility == View.VISIBLE)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun fullscreen_disabledOnPortrait() {
        rule.runOnUiThread {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        rule.runOnUiThread {
            val decorView = rule.activity.window.decorView
            assertThat(
                decorView.systemUiVisibility,
                not(hasFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)),
            )
        }
    }

    private fun isPlaying(): Matcher<in View> =
        object : TypeSafeMatcher<View>() {
            override fun matchesSafely(view: View): Boolean =
                (view as PlayerView).player?.playbackState == Player.STATE_READY && view.player?.playWhenReady ?: false

            override fun describeTo(description: Description) {
                description.appendText("PlayerView is playing")
            }
        }

    //
    private fun showControls(): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(PlayerView::class.java)

            override fun getDescription(): String = "Show controls of PlayerView"

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                uiController.loopMainThreadUntilIdle()
                (view as PlayerView).showController()
                uiController.loopMainThreadUntilIdle()
            }
        }

    private fun hasFlag(flag: Int): Matcher<in Int> =
        object : TypeSafeMatcher<Int>() {
            override fun matchesSafely(i: Int?): Boolean = i?.and(flag) == flag

            override fun describeTo(description: Description) {
                description.appendText("Flag integer contains " + flag)
            }
        }

    private class ExoPlayerIdlingResource(
        private val player: Player
    ) : IdlingResource {
        @Volatile
        private var callback: IdlingResource.ResourceCallback? = null
        private val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    callback?.onTransitionToIdle()
                }
            }
        }

        override fun getName(): String = "ExoPlayerIdlingResource"

        override fun isIdleNow(): Boolean {
            val isIdle = player.playbackState == Player.STATE_READY
            if (isIdle) {
                callback?.onTransitionToIdle()
            }
            return isIdle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
            player.addListener(listener)
        }

        fun unregister() {
            player.removeListener(listener)
        }
    }
}

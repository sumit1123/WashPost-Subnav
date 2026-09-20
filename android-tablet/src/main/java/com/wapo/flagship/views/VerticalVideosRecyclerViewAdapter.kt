// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.views

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.sentenceCase
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.common.getAdTagUrl
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.flagship.features.video.models.VerticalVideoAdItem
import com.wapo.flagship.features.video.models.VideoAdResponseState
import com.wapo.flagship.features.video.viewmodels.NativeVideoAdViewModel
import com.wapo.flagship.features.wpvideos.fragments.WatchVideoFragment.Companion.WP_VIDEO_BUNDLE_NAME
import com.wapo.flagship.features.wpvideos.models.WatchVideoAdItem
import com.wapo.flagship.util.UIUtil
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.view.RippleHelper
import com.washingtonpost.android.R
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

const val VerticalVideosPlayerName = "VerticalVideos"

class VerticalVideosRecyclerViewAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val videosAdViewModel: NativeVideoAdViewModel,
    private val sourceScreen: String? = null,
    private val playNextButtonClickListener: (Int) -> Unit,
) : ListAdapter<Any, VerticalVideosRecyclerViewAdapter.VerticalVideoBaseViewHolder>(DiffUtils()) {
    class DiffUtils : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(
            oldItem: Any,
            newItem: Any,
        ): Boolean = oldItem === newItem

        override fun areContentsTheSame(
            oldItem: Any,
            newItem: Any,
        ): Boolean {
            // Usually VerticalVideoAdItem.kt content is same for all ads.
            // want to make sure ad items should not be treated same as long as their references are different.
            // Saw a case when ad item data is same for all ad positions, adapter is using the same item for other ad position also.
            // It is good to have different data for each item as a general rule.
            // Anyways control doesn't come here when areItemsTheSame returns false.
            return oldItem === newItem
        }
    }

    open class VerticalVideoBaseViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        open fun unbind() {}
    }
    inner class VerticalVideosLoadingViewHolder(
        view: View
    ): VerticalVideoBaseViewHolder(view){

    }

    open inner class VerticalVideoViewHolder(
        private val view: View,
    ) : VerticalVideoBaseViewHolder(view) {
        private val mediaContainer: ViewGroup = view.findViewById(R.id.media_container)
        private val videoContainer: ViewGroup = view.findViewById(R.id.video_container)
        open val progressSpinner: ProgressBar = view.findViewById(R.id.progress_spinner)
        private val promoImage: ImageView = view.findViewById(R.id.promo_image)
        private val promoHeadLineContainer: ViewGroup =
            view.findViewById(R.id.promo_headline_container)
        private val promoHeadLine: TextView = view.findViewById(R.id.promo_headline)
        private val initialCoachView: View = view.findViewById(R.id.initial_coach_mark)
        private val coachArrow: ImageView = initialCoachView.findViewById(R.id.initial_coach_arrow)
        private val coachLabel: TextView = initialCoachView.findViewById(R.id.initial_coach_label)
        private val countDownTimerView: View = view.findViewById(R.id.countdown_timer)
        open val playNextButton: Button = countDownTimerView.findViewById(R.id.play_next_button)
        private val timerValue: TextView = countDownTimerView.findViewById(R.id.timer_value)
        private val timerIndicator: CircularProgressIndicator =
            countDownTimerView.findViewById(R.id.timer_indicator)
        open var totalItems: Int = 0
        open val player2Manager = PostTvPlayer2Coordinator.getPlayer(VerticalVideosPlayerName)
        open val controllerViewContainer: ViewGroup? =
            player2Manager.getPlayerContainerView()?.findViewById(R.id.controller_view_container)
        private var playbackStateObserver: Observer<PlaybackState>? = null
        private var showInitialCoach = AtomicBoolean()
        private var showHeadlineOnVideo = AtomicBoolean()
        private val videoHeadline: TextView? =
            player2Manager
                .getPlayerContainerView()
                ?.findViewById<TextView>(R.id.video_headline)
                ?.apply {
                    typeface =
                        ResourcesCompat.getFont(itemView.context, com.wapo.view.R.font.wp_postoniwide_font_family)
                }
        private val videoHeadlineDelayHandler =
            Handler(Looper.getMainLooper()) {
                videoHeadline?.visibility = View.GONE
                true
            }
        private val containerWidth =
            Math.max(
                (AppContextUtils.getSmallestScreenWidth() * 0.5625f).toInt(),
                itemView.resources.getDimensionPixelSize(R.dimen.vertical_videos_max_container_width),
            )
        private var countdownTimer: CountDownTimer =
            object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerValue.text = (millisUntilFinished.toDouble() / 1000).toInt().toString()
                }

                override fun onFinish() {
                    isTimerRunning = false
                    playNextButton.performClick()
                    resetTimerUi()
                }
            }
        private var isTimerRunning = false

        fun bindVideoItem(
            verticalVideo: Video,
            totalCount: Int,
            position: Int,
        ) {
            promoHeadLine.layoutParams?.width = containerWidth
            controllerViewContainer?.layoutParams?.width = containerWidth

            if (!verticalVideo.mediaUrl.isNullOrEmpty()) {
                Glide
                    .with(FlagshipApplication.getInstance().applicationContext)
                    .asBitmap()
                    .load(verticalVideo.mediaUrl)
                    .into(promoImage)
            }
            totalItems = totalCount
            promoHeadLine.text = verticalVideo.headline
            animateCoachMark()
            playNextButton.setOnClickListener {
                playNextButtonClickListener(position + 1)
            }
        }

        override fun unbind() {
            initialCoachView.visibility = View.INVISIBLE
            promoImage.visibility = View.VISIBLE
            promoHeadLineContainer.visibility = View.VISIBLE
            videoHeadlineDelayHandler.removeMessages(0)
            removeObserver()
            countdownTimer.cancel()
            resetTimerUi()
        }

        private fun removeObserver() {
            playbackStateObserver?.let {
                player2Manager.playbackState.removeObserver(it)
            }
            playbackStateObserver = null
        }

        open fun playVideo(verticalVideo: Video) {
            playVideoDirectly(verticalVideo)
        }

        private fun playVideoDirectly(video: Video) {
            val watchAdTagUrl = if (!FlagshipApplication.getInstance().shouldSuppressAds()) {
                getAdTagUrl(video, sourceScreen)
            } else null
            val verticalVideo = video.copy(adTagUrl = watchAdTagUrl)
            player2Manager.apply {
                addPlayerContainerViewToItemView(videoContainer)
                removeObserver()
                observePlayerState(verticalVideo)
                hideController()
                val tracking =
                    VideoTracker2.Tracking(
                        progressThreshold = videosAdViewModel.getTrackingPosition(adapterPosition),
                        VideoTracker2.VideoType.VERTICAL_FULLSCREEN,
                        video = verticalVideo
                    )
                playMedia(verticalVideo, tracking = tracking)
            }
            updateVideoHeadline(verticalVideo)
        }

        private fun observePlayerState(verticalVideo: Video) {
            val playerView: PlayerView = view.findViewById(R.id.player_view)
            playbackStateObserver =
                Observer<PlaybackState> { state ->
                    when (state) {
                        is PlaybackState.Buffering -> {
                            if (player2Manager.getPlaybackPosition() == 0L) {
                                playerView.visibility = View.INVISIBLE
                                playerView.videoSurfaceView?.visibility = View.VISIBLE
                                promoImage.visibility = View.VISIBLE
                                promoHeadLineContainer.visibility = View.INVISIBLE
                                if (!showInitialCoach.get()) {
                                    initialCoachView.visibility = View.INVISIBLE
                                }
                            }
                            progressSpinner.visibility = View.VISIBLE
                        }
                        is PlaybackState.Ready -> {
                            playerView.visibility = View.VISIBLE
                            promoImage.visibility = View.INVISIBLE
                            promoHeadLineContainer.visibility = View.INVISIBLE
                            progressSpinner.visibility = View.INVISIBLE
                            player2Manager.showController()

                            if (!player2Manager.isPlayingAds()) {
                                showInitialCoach.set(true)
                                showHeadlineOnVideo.set(true)
                            }

                            // Show InitialCoach only for the first 2 seconds on all the items except on the last item.
                            if (showInitialCoach.get() && adapterPosition < totalItems - 2 && !player2Manager.isPlayingAds()) {
                                showInitialCoach.set(false)
                                initialCoachView.visibility = View.VISIBLE
                                initialCoachView.postDelayed({
                                    initialCoachView.visibility = View.INVISIBLE
                                }, 2000)
                            }
                            if (showHeadlineOnVideo.get()) {
                                showHeadlineOnVideo.set(false)
                                videoHeadlineDelayHandler.sendEmptyMessageDelayed(0, 2000)
                            }
                        }
                        is PlaybackState.ShowCountdownTimer -> {
                            if (AppPreferences.isAutoplayVideosOn() &&
                                !isTimerRunning &&
                                adapterPosition < totalItems - 2 &&  !player2Manager.isPlayingAds()
                            ) {
                                isTimerRunning = true
                                countdownTimer.start()
                                countDownTimerView.visibility = View.VISIBLE
                                coachArrow.visibility = View.INVISIBLE
                                coachLabel.visibility = View.INVISIBLE
                                animateTimerIndicator()
                            }
                        }
                        is PlaybackState.CancelCountdownTimer -> {
                            if (isTimerRunning) {
                                isTimerRunning = false
                                countdownTimer.cancel()
                                resetTimerUi()
                            }
                        }
                        is PlaybackState.Ended -> {
                            promoImage.visibility = View.VISIBLE
                            updateVideoHeadline(verticalVideo)
                            /* Hide the video surface view so that just the controller view and
                               promo image are visible */
                            playerView.videoSurfaceView?.visibility = View.INVISIBLE
                            // Check to hide the initial coach for the last video
                            if (adapterPosition < totalItems - 2 &&  !player2Manager.isPlayingAds()) {
                                initialCoachView.visibility = View.VISIBLE
                            }
                        }
                        is PlaybackState.Idle -> {
                            progressSpinner.visibility = View.INVISIBLE
                        }
                        else -> {
                            // no op
                        }
                    }
                }.also {
                    player2Manager.playbackState.observe(lifecycleOwner, it)
                }
        }
        /***
         * Animating coach mark to make it a little bounce {For the user to know to swipe more}
         * ObjectAnimator.Reverse is added to make the animation to reverse to make it to and fro
         * Duration is set to 400, Defines the speed for the Animator
         * Using the Liner Inter-polar as the rate of change is constant
         */
        open fun animateCoachMark() {
            ObjectAnimator
                .ofFloat(coachArrow, "translationY", -5f, 10f)
                .apply {
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                    duration = 400
                    interpolator = LinearInterpolator()
                }.start()
        }

        private fun animateTimerIndicator() {
            ObjectAnimator
                .ofInt(timerIndicator, "progress", 0, 100)
                .setDuration(5000)
                .start()
        }

        private fun updateVideoHeadline(video: Video) {
            videoHeadlineDelayHandler.removeMessages(0)
            videoHeadline?.apply {
                text = video.headline
                visibility = View.INVISIBLE
            }
        }

        private fun resetTimerUi() {
            timerValue.text = "5"
            timerIndicator.progress = 0
            countDownTimerView.visibility = View.GONE
            coachArrow.visibility = View.VISIBLE
            coachLabel.visibility = View.VISIBLE
        }

        open fun updateControls() {
            player2Manager.showControllerOptions(com.wapo.flagship.features.posttv.R.id.exo_share)
            controllerViewContainer?.apply {
                setPadding(paddingLeft, paddingTop, paddingRight, 0)
            }
        }
    }

    inner class VerticalVideosAdCardViewHolder(
        view: View,
    ) : VerticalVideoViewHolder(view) {
        private val tvAdvertisement: TextView = view.findViewById(R.id.tv_advertisement)
        private val buttonCta: Button = view.findViewById(R.id.button_cta)
        private val controlsBottomPadding = UIUtil.dip2Px(56, view.context).toInt()
        private var adResponseStateObserver: Observer<VideoAdResponseState?>? = null
        private var trackingPlaybackStateObserver: Observer<PlaybackState>? = null
        private val trackFiredEvents = LinkedList<PlaybackState>()

        fun bindAdItem(
            position: Int,
        ) {
            bindVideoItem(Video.Builder().build(), itemCount, position)
        }

        fun playVideo() {
            if (player2Manager.isPlaying()) {
                player2Manager.pauseMedia()
            }
            adResponseStateObserver =
                Observer<VideoAdResponseState?> { adState ->
                    when (adState) {
                        is VideoAdResponseState.Success -> {
                            val adResponse = (adState as? VideoAdResponseState.Success)?.adResponse
                            val videoUrl = adResponse?.videoUrl
                            val ctaText = adResponse?.ctaButtonText
                            val ctaButtonColor = adResponse?.ctaButtonHexColor
                            if (!ctaText.isNullOrEmpty()) {
                                buttonCta.apply {
                                    var bgColor = Color.WHITE
                                    if (!ctaButtonColor.isNullOrEmpty()) {
                                        setTextColor(Color.WHITE)
                                        bgColor = Color.parseColor(ctaButtonColor)
                                    } else {
                                        setTextColor(Color.BLACK)
                                    }
                                    val bgDrawable =
                                        GradientDrawable().also {
                                            it.setColor(bgColor)
                                            it.cornerRadius = UIUtil.dip2Px(32, context)
                                        }
                                    background = bgDrawable
                                    text = SpannableString(ctaText).sentenceCase()
                                    RippleHelper.addRippleEffectToView(buttonCta)
                                    setOnClickListener {
                                        videosAdViewModel
                                            .getAdResponse(absoluteAdapterPosition)
                                            ?.nativeCustomFormatAd
                                            ?.performClick(ctaText)
                                    }
                                }
                                buttonCta.visibility = View.VISIBLE
                            } else {
                                buttonCta.visibility = View.GONE
                            }
                            if (!videoUrl.isNullOrEmpty()) {
                                val pageName = if (sourceScreen == WP_VIDEO_BUNDLE_NAME) Measurement.PAGE_WATCH_VIDEO else sourceScreen
                                val video =
                                    Video
                                        .Builder()
                                        .setId(videoUrl)
                                        .setContentUrl(videoUrl)
                                        .setSource(adResponse)
                                        .setPageName(pageName)
                                        .build()
                                playVideo(video)
                                startTrackingPlaybackStateObserver()
                                videosAdViewModel.getAdResponse(absoluteAdapterPosition)?.nativeCustomFormatAd?.recordImpression()
                                videosAdViewModel.getAdResponse(absoluteAdapterPosition)?.impressionPixels?.forEach { pixel ->
                                    videosAdViewModel.sendPixelTrackingRequest(pixel, videoUrl)
                                }
                            }
                            updateControls()
                        }
                        else -> {}
                    }
                }.also {
                    videosAdViewModel.getAdState(absoluteAdapterPosition)?.observe(lifecycleOwner, it)
                }
        }

        override fun unbind() {
            adResponseStateObserver?.let {
                videosAdViewModel.getAdState(absoluteAdapterPosition)?.removeObserver(it)
            }
            adResponseStateObserver = null
            trackingPlaybackStateObserver?.let {
                player2Manager.playbackState.removeObserver(it)
            }
            trackingPlaybackStateObserver = null
            // Tracking pause and completion pixels.
            val adResponse = videosAdViewModel.getAdResponse(absoluteAdapterPosition)
            if (player2Manager.playbackState.value == PlaybackState.CancelCountdownTimer &&
                trackFiredEvents.lastOrNull() != PlaybackState.Paused
            ) {
                videosAdViewModel.sendPixelTrackingRequest(
                    adResponse?.videoPausePixel,
                    adResponse?.videoUrl,
                )
            } else if (player2Manager.playbackState.value == PlaybackState.ShowCountdownTimer &&
                trackFiredEvents.lastOrNull() != PlaybackState.Ended
            ) {
                videosAdViewModel.sendPixelTrackingRequest(
                    adResponse?.videoCompletionPixel,
                    adResponse?.videoUrl,
                )
            }
            trackFiredEvents.clear()
            buttonCta.visibility = View.GONE
            super.unbind()
        }

        override fun updateControls() {
            player2Manager.hideControllerOptions(com.wapo.flagship.features.posttv.R.id.exo_share)
            controllerViewContainer?.apply {
                if (buttonCta.visibility == View.VISIBLE) {
                    setPadding(paddingLeft, paddingTop, paddingRight, controlsBottomPadding)
                }
            }
        }

        private fun startTrackingPlaybackStateObserver() {
            trackingPlaybackStateObserver =
                Observer<PlaybackState> {
                    val adResponse = videosAdViewModel.getAdResponse(absoluteAdapterPosition)
                    when (it) {
                        is PlaybackState.Ready, PlaybackState.Resumed -> {
                            if (trackFiredEvents.lastOrNull() !is PlaybackState.Ready) {
                                videosAdViewModel.sendPixelTrackingRequest(
                                    adResponse?.videoPlayPixel,
                                    adResponse?.videoUrl,
                                )
                                trackFiredEvents.add(PlaybackState.Ready)
                            }
                        }
                        is PlaybackState.Ended -> {
                            if (trackFiredEvents.lastOrNull() !is PlaybackState.Ended) {
                                videosAdViewModel.sendPixelTrackingRequest(
                                    adResponse?.videoCompletionPixel,
                                    adResponse?.videoUrl,
                                )
                                trackFiredEvents.add(PlaybackState.CancelCountdownTimer)
                                trackFiredEvents.add(PlaybackState.Ended)
                            }
                        }
                        is PlaybackState.Paused -> {
                            if (trackFiredEvents.lastOrNull() !is PlaybackState.Paused) {
                                videosAdViewModel.sendPixelTrackingRequest(
                                    adResponse?.videoPausePixel,
                                    adResponse?.videoUrl,
                                )
                                trackFiredEvents.add(PlaybackState.Paused)
                            }
                        }
                        else -> {}
                    }
                }.also {
                    player2Manager.playbackState.observe(lifecycleOwner, it)
                }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int,
    ): VerticalVideoBaseViewHolder =
        if (viewType == VIDEO_TYPE) {
            val view =
                LayoutInflater
                    .from(viewGroup.context)
                    .inflate(R.layout.vertical_video_media_container, viewGroup, false)
            VerticalVideoViewHolder(view)
        } else if (viewType == AD_TYPE) {
            val view =
                LayoutInflater.from(viewGroup.context).inflate(
                    R.layout.vertical_video_ad_media_container,
                    viewGroup,
                    false,
                )
            VerticalVideosAdCardViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(viewGroup.context).inflate(
                    R.layout.vertical_video_loading_card,
                    viewGroup,
                    false,
                )
            VerticalVideosLoadingViewHolder(view)
        }

    override fun onBindViewHolder(
        holder: VerticalVideoBaseViewHolder,
        position: Int,
    ) {
        if (getItemViewType(position) == VIDEO_TYPE) {
            (holder as VerticalVideoViewHolder).bindVideoItem(
                getItem(position) as Video,
                itemCount,
                position,
            )
        } else if (getItemViewType(position) == AD_TYPE) {
            (holder as VerticalVideosAdCardViewHolder).bindAdItem(
                position,
            )
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is Video) {
            VIDEO_TYPE
        } else if (getItem(position) is VerticalVideoAdItem || getItem(position) is WatchVideoAdItem) {
            AD_TYPE
        } else -1

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= itemCount) {
            return RecyclerView.NO_ID
        }
        if (getItemViewType(position) == VIDEO_TYPE) {
            (getItem(position) as Video).id.hashCode().toLong().let { verticalVideoHash ->
                return verticalVideoHash
            }
        } else {
            return getItem(position).hashCode().toLong()
        }
    }

    companion object {
        const val VIDEO_TYPE = 0
        const val AD_TYPE = 2
    }
}

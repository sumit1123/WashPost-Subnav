package com.wapo.flagship.features.articles2.viewholders

import android.annotation.SuppressLint
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.formatTimeMillis
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.ads.targeting.TargetingContent
import com.wapo.flagship.features.articles2.ads.targeting.TargetingContentUIState
import com.wapo.flagship.features.articles2.models.Article2
import com.wapo.flagship.features.articles2.models.deserialized.video.Host
import com.wapo.flagship.features.articles2.models.deserialized.video.Video
import com.wapo.flagship.features.articles2.placeholder.PlaceHolderData
import com.wapo.flagship.features.articles2.utils.VideoStyleHelper
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.sections.utils.UIUtils
import com.wapo.flagship.features.video.viewmodels.VideoActivityViewModel
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ItemVideoBinding
import com.washingtonpost.android.paywall.util.EMPTY_STRING
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel
import java.util.concurrent.TimeUnit

class VideoViewHolder(
    private val binding: ItemVideoBinding,
    private val articleModel: Article2,
    private val videoActivityViewModel: VideoActivityViewModel,
    private val userHistoryViewModel: UserHistoryViewModel,
    private val lifecycleOwner: LifecycleOwner,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder<Video>(
    binding.root,
    binding.placeholder,
) {
    companion object {
        const val ARTICLES_SECTION = "articles"
    }
    private val videoManager2 = videoActivityViewModel.getVideoManager2()
    private var videosLoadedEventObserver: Observer<Pair<String, String>>? = null
    var item: Video? = null
    private var videoId: String? = null
    var isVerticalVideo = false

    @SuppressLint("DefaultLocale")
    override fun setPlaceHolderData(item: Video): PlaceHolderData? {
        val durationString =
            item.duration?.let {
                String.format(
                    "%01d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(it), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(it) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(it)),
                )
            } ?: EMPTY_STRING

        val resources = binding.root.context.resources
        return PlaceHolderData(
            message = resources.getString(R.string.low_data_mode_video_message),
            reloadLabel =
                resources.getString(
                    R.string.low_data_mode_video_button_label,
                    durationString,
                ),
            reloadIcon = com.wpds.wpds.R.drawable.play,
            aspectRatio = getAspectRatio(item),
        ) {
            setVideo(item)
        }
    }

    override fun onLowDataModeEnable(item: Video) {
        binding.apply {
            videoContainer.isVisible = false
            placeholder.isVisible = true
        }
    }

    override fun onLowDataModeDisable(item: Video) {
        setVideo(item)
    }

    fun resetVideo() {
        item?.let {
            videosLoadedEventObserver?.let {
                videoActivityViewModel.videoLoadedEvent.removeObserver(it)
            }
            videosLoadedEventObserver = null
            val player2Manager = videoManager2.getPlayerManager(videoId)
            player2Manager?.playbackState?.removeObservers(lifecycleOwner)
            if (player2Manager?.playbackState?.value is PlaybackState.Error) {
                videoManager2.release(
                    videoId,
                )
            }
            val playerFrame = videoManager2.getPlayerFrameContainer(videoId)
            if (playerFrame != null && playerFrame.parent === binding.videoContainer) {
                videoManager2.removePlayerFrame(videoId)
            }
            setVideo(it)
        }
    }

    private fun setVideo(item: Video) {
        binding.apply {
            videoContainer.isVisible = true
            item.isItemAlreadyShowed = true
            placeholder.isVisible = false
            val params = proportionalLayout.layoutParams
            val aspectRatio = getAspectRatio(item)
            if (aspectRatio < 1) {
                params.width = UIUtils.dpToPx(300f, itemView.resources)
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else if (aspectRatio == 1.0f) {
                params.width = UIUtils.dpToPx(300f, itemView.resources)
                params.height = UIUtils.dpToPx(300f, itemView.resources)
            } else {
                params.width = LayoutParams.MATCH_PARENT
                params.width = LayoutParams.MATCH_PARENT
            }

            this@VideoViewHolder.item = item
            this@VideoViewHolder.videoId = getVideoId(item)
            isVerticalVideo = item.vertical == true
            setImagePreviewForVideo(item)
            setVideoCaption(item)
            adjustDurationView()
            proportionalLayout.layoutParams = params
            proportionalLayout.aspectRatio = aspectRatio
            // Attach Video if it is already loaded and playing.
            // Basically when view holder is recycled, app will remove the player frame from view holder
            // and attach here when view holder should display any video for current item position.
            // Autoplay videos also can be attached once they satisfy all autoplay conditions.
            displayVideoIfRequired(item)
            // Observe for VideoLoadedEvent to start autoplay the video once video items are loaded
            // and video is eligible for autoplay.
            observeVideoLoadedEvent(item)
            determineOverlay(item)
        }
    }

    /**
     * Adjusting duration to be center vertical if the headline is empty or baked-in scenario
     */
    private fun adjustDurationView() {
        if (isVerticalVideo) {
            if (binding.headline.text.isEmpty()) {
                binding.headline.visibility = View.GONE
                val params = binding.duration.layoutParams as ConstraintLayout.LayoutParams
                params.topMargin =
                    binding.root.context.resources.getDimensionPixelOffset(
                        com.washingtonpost.android.articles.R.dimen.articles_small_margin,
                    )
                binding.duration.layoutParams = params
            } else {
                binding.headline.visibility = View.VISIBLE
                val params = binding.duration.layoutParams as ConstraintLayout.LayoutParams
                params.topMargin =
                    binding.root.context.resources.getDimensionPixelOffset(
                        com.washingtonpost.android.articles.R.dimen.articles_brief_small_margin,
                    )
                binding.duration.layoutParams = params
            }
        }
    }

    /**
     * Call from onAttachedToWindow from Adapter
     */
    fun bindMedia() {
        // When ViewHolder is off/on the screen,
        // App will not pause and resume user initiated videos as they should continue to play as
        // long as user is on that article and video is not ended.
        // But to save resources, app will pause and resume autoplay videos.
        if (isAutoplayLoopingVideo()) {
            videoManager2.incrementAutoplayCount()
            if (isAutoplayVideo()) {
                videoManager2.getPlayerManager(videoId)?.run {
                    if (!isPlaying()) setPlayWhenReady(true)
                }
            }
        }
    }

    /**
     * Call from onDetachedFromWindow from Adapter
     */
    fun unbindMedia() {
        val videoManager = videoManager2.getPlayerManager(videoId)
        videoManager?.run {
            if (isAutoplayLoopingVideo()) {
                videoManager2.decrementAutoplayCount()
                videoManager2.release(videoId)
            }
            if (isAutoplayVideo()) {
                if (isPlaying() && !videoManager.isPlayingAds()) {
                    //Queue auto played video event to storage when a user scrolls past video
                    video?.let {
                        if (it.aspectRatio < 1) {
                            userHistoryViewModel.writeVideoViewedEvent(
                                id = it.contentId,
                                section = ARTICLES_SECTION,
                                autoplayDuration = this.getPlaybackPosition(),
                                watchDuration = null,
                                totalMilliSeconds = getDuration(),
                                videoConclusionState = VideoConclusionState.SCROLLED_THROUGH
                            )
                        }
                    }
                    setPlayWhenReady(false)
                }
            } else if (isPlaying() && !videoManager.isPlayingAds()) {
                //Queue video event to storage if a video isn't auto played but user plays video
                video?.let {
                    if (it.aspectRatio < 1) {
                        userHistoryViewModel.writeVideoViewedEvent(
                            id = it.contentId,
                            section = ARTICLES_SECTION,
                            autoplayDuration = null,
                            watchDuration = getPlaybackPosition(),
                            totalMilliSeconds = getDuration(),
                            videoConclusionState = VideoConclusionState.SCROLLED_THROUGH
                        )
                    }
                }
            }
        }
    }

    /**
     * Call from onViewRecycled from Adapter. Release all vh resources as it can be reused for other
     * videos. But not releasing the player and its resources until user leaves the article.
     * So video player and frame can be re-attached while scrolling.
     */
    fun onViewRecycled() {
        releaseResources()
    }

    fun releaseResources() {
        videosLoadedEventObserver?.let {
            videoActivityViewModel.videoLoadedEvent.removeObserver(it)
        }
        videosLoadedEventObserver = null
        val player2Manager = videoManager2.getPlayerManager(videoId)
        player2Manager?.playbackState?.removeObservers(lifecycleOwner)
        if (player2Manager?.playbackState?.value is PlaybackState.Error) {
            videoManager2.release(
                videoId,
            )
        }
        val playerFrame = videoManager2.getPlayerFrameContainer(videoId)
        if (playerFrame != null && playerFrame.parent === binding.videoContainer) {
            videoManager2.removePlayerFrame(videoId)
        }
        videoId = null
        item = null
    }

    private fun bindAutoplayVideo(item: Video) {
        // Skip Autoplay when
        // 1. user has already initiated that video
        // 2. autoplay is off in the app settings
        // 3. Article (cached/changes are not live yet) that has no autoplay values
        // 4. autoplay value is off and there is no promo url in the feed
        // 5. mediaType is youtube or vimeo
        // 6. No network
        if (videoManager2.getPlayerFrame(getVideoId(item))?.video?.playType ==
            com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL
        ) {
            return
        }
        if (!videoActivityViewModel.canAutoPlayInlineVideo()) return
        if (item.autoplay == null && item.isLooping == null && item.promo?.url.isNullOrEmpty()) return
        if (item.autoplay == false && item.promo?.url.isNullOrEmpty()) return
        if (item.host == Host.YOUTUBE || item.host == Host.VIMEO) return
        if (!AppContextUtils.isConnectingOrConnected()) return
        if (!videoManager2.canAddAutoplay()) return
        val playType =
            if (item.autoplay == true && item.isLooping == true) {
                com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED
            } else {
                com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_AUTOPLAY
            }
        bindVideo(item, playType, null)
    }

    fun bindUserInitiatedVideo(item: Video, targetingContent: TargetingContent?) {
        // Skip allowing [Video.PLAY_TYPE_NORMAL_MUTED] videos when autoplay is enabled in the app settings.
        // They will autoplay and loop based on their view holders visibility.
        val playType =
            if (item.autoplay == true && item.isLooping == true) {
                com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED
            } else {
                com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL
            }
        if (playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED &&
            videoActivityViewModel.canAutoPlayInlineVideo()
        ) {
            return
        }
        bindVideo(item, playType, targetingContent)
        hideOverlay()
    }

    private fun getAdTagUrl(videoData: Video, targetingContent: TargetingContent?): String? =
        com.wapo.flagship.common.getAdTagUrl(
            videoData,
            articleModel,
            targetingContent
        )

    private fun bindVideo(
        videoData: Video,
        playType: Int,
        targetingContent: TargetingContent?
    ) {
        val adTagUrl =
            if (playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL
                && videoData.vertical == false
                && !FlagshipApplication.getInstance().shouldSuppressAds()
            )
                com.wapo.flagship.common.getAdTagUrl(
                    videoData,
                    articleModel,
                    targetingContent
                ) else null

        val aspectRatio = getAspectRatio(videoData)
        val video = com.wapo.flagship.features.posttv.model.Video
            .Builder()
            .setId(getVideoId(videoData))
            .setContentUrl(videoData.contenturl)
            .setShareUrl(videoData.shareurl)
            .setHeadline(null)
            .setIsYouTube(videoData.host == Host.YOUTUBE)
            .setIsVimeo(videoData.host == Host.VIMEO)
            .setDuration(videoData.duration ?: 0L)
            .setIsLive(
                videoData.isLive ?: (
                        videoData.duration == null &&
                                videoData.content?.duration == null
                        ),
            ).setPageName(videoData.omniture?.pageName)
            .setVideoName(videoData.title)
            .setVideoSection(videoData.omniture?.contentSubsection)
            .setVideoSource(videoData.omniture?.source)
            .setVideoCategory(null)
            .setShouldPlayAds(
                videoData.adconfig?.playAds == true && !FlagshipApplication.getInstance()
                    .shouldSuppressAds()
            )
            .setContentId(videoData.omniture?.contentId)
            .setSubtitleUrl(videoData.subtitlesURL)
            .setFallbackUrl(videoData.fallback)
            .setAspectRatio(aspectRatio)
            .setIsLooping(videoData.isLooping == true)
            .setAutoplay(videoData.autoplay == true)
            .setPromoIsLooping(videoData.promo?.isLooping == true)
            .setPromoUrl(videoData.promo?.url)
            .setPlayType(playType)
            .setSource(videoData)
            .setAdTagUrl(adTagUrl)
            .setPlaybackPosition(getPlaybackPosition(playType))
            .setArcId(articleModel.arcId)
            .build()

        videoManager2.initMedia(video, isPlayerClickable = videoData.vertical == true, {
            videoData.vertical?.let {
                if (it) {
                    video.adTagUrl = if (!FlagshipApplication.getInstance().shouldSuppressAds()) {
                        getAdTagUrl(videoData, targetingContent)
                    } else null
                    video.playbackPosition =
                        getPlaybackPosition(com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL)
                    videoActivityViewModel.openWatchVideoCard(video)
                }
            }
        })


        if (playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_AUTOPLAY ||
            playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED
        ) {
            displayVideo(videoId, whenReady = true)
        } else {
            displayVideo(videoId)
        }
    }

    private fun displayVideo(
        videoId: String?,
        whenReady: Boolean = false,
    ) {
        val playerFrame = videoManager2.getPlayerFrameContainer(videoId)
        if (playerFrame != null && playerFrame.parent !== binding.videoContainer) {
            videoManager2.removePlayerFrame(videoId)
            playerFrame.tag = itemId
            if (whenReady) {
                binding.videoContainer.addView(playerFrame, 0)
            } else {
                binding.videoContainer.addView(playerFrame)
            }
            observePlaybackStateEvents()
        }
    }

    private fun displayVideoIfRequired(item: Video) {
        val videoId = getVideoId(item)
        val isInPIP = videoManager2.getPlayerFrame(videoId)?.isInPIP == true
        val playerFrame: FrameLayout? = videoManager2.getPlayerFrameContainer(videoId)

        val shouldVideoDisplay = playerFrame != null && playerFrame.parent == null
        val isAttachedToThisHolder = playerFrame?.parent === binding.videoContainer
        val isAttachedToAnotherHolder = !isAttachedToThisHolder && playerFrame?.parent != null
        if (shouldVideoDisplay || (isAttachedToAnotherHolder && !isInPIP)) {
            displayVideo(videoId)
            playerFrame?.bringToFront()
        } else if (playerFrame == null) {
            bindAutoplayVideo(item)
        } else if (isAttachedToThisHolder && !isInPIP) {
            playerFrame.bringToFront()
        }
        updateOverlay()
    }

    /**
     * Observe for player events to update player frame and overlay.
     * Player frame can be moved to front once video is ready. So there will no blank views.
     */
    private fun observePlaybackStateEvents() {
        val playerFrame = videoManager2.getPlayerFrameContainer(videoId)
        val player2Manager = videoManager2.getPlayerManager(videoId)
        player2Manager?.playbackState?.removeObservers(lifecycleOwner)
        player2Manager?.playbackState?.observe(lifecycleOwner) { playbackState ->
            if (videoId != this@VideoViewHolder.videoId) {
                return@observe
            }
            when (playbackState) {
                is PlaybackState.Ready -> {
                    playerFrame?.bringToFront()
                    updateOverlay()
                }

                is PlaybackState.Ended -> {
                    updateOverlay()
                }

                else -> {}
            }
        }
    }

    private fun determineOverlay(item: Video) {
        if (item.host == Host.VIMEO || item.host == Host.YOUTUBE) {
            binding.videoPlayButton.visibility = View.VISIBLE
            binding.videoOverlayScreen.visibility = View.INVISIBLE
        } else {
            binding.videoPlayButton.visibility = View.INVISIBLE
            binding.videoOverlayScreen.visibility = View.VISIBLE

            if (item.isLive == true) {
                binding.playBtn.setImageResource(com.washingtonpost.android.articles.R.drawable.btn_live_article)
                binding.duration.setCompoundDrawablesWithIntrinsicBounds(
                    com.washingtonpost.android.sections.R.drawable.drawable_solid_circle_red,
                    0,
                    0,
                    0,
                )
                binding.headline.text = item.title
                binding.duration.text = itemView.context.getText(com.washingtonpost.android.articles.R.string.article_live_video)
            } else {
                binding.playBtn.setImageResource(com.washingtonpost.android.articles.R.drawable.btn_play_article)
                binding.duration.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                binding.headline.text = item.title
                binding.duration.text = formatTimeMillis(item.duration)
            }
        }
    }

    private fun observeAdsContentUiStateEvents(item: Video) {
        if (item.id.isNullOrEmpty()) return
        val videoId = item.id
        videoActivityViewModel.createAdsContentUiState(item.id)
        val uiState = videoActivityViewModel.targetingContentUiState[videoId]
        uiState?.removeObservers(lifecycleOwner)
        uiState?.observe(lifecycleOwner) { state ->
            state ?: return@observe
            if (videoId != state.id) return@observe
            when (state) {
                is TargetingContentUIState.Loading -> {
                    showProgressBar()
                }

                is TargetingContentUIState.Cancelled -> {
                    uiState.removeObservers(lifecycleOwner)
                    videoActivityViewModel.clearAdsContentUiState(videoId)
                    hideProgressBar()
                }

                is TargetingContentUIState.Content, is TargetingContentUIState.Error, is TargetingContentUIState.UITimeout -> {
                    val targetingContentItem =
                        if (state is TargetingContentUIState.Content) state.items.firstOrNull { it.id == videoId } else null
                    bindUserInitiatedVideo(item, targetingContentItem)
                    uiState.removeObservers(lifecycleOwner)
                    videoActivityViewModel.clearAdsContentUiState(videoId)
                    hideProgressBar()
                }
            }
        }
    }

    fun updateOverlay() {
        // Hide overlay for autoplay and looping enabled, and user initiated videos.
        val player2Manager = videoManager2.getPlayerManager(videoId)
        val shouldHideOverlay =
            player2Manager?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED ||
                    player2Manager?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL ||
                    (videoActivityViewModel.canAutoPlayInlineVideo() && item?.autoplay == true && item?.isLooping == true) ||
                    (
                            player2Manager?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_AUTOPLAY &&
                                    videoActivityViewModel.canAutoPlayInlineVideo() &&
                                    item?.autoplay == true &&
                                    item?.isLooping == false
                            )
        if (shouldHideOverlay) hideOverlay() else bringOverlayToFront()
    }

    private fun bringOverlayToFront() {
        binding.videoOverlayContainer.bringToFront()
        binding.videoOverlayContainer.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        binding.videoOverlayContainer.visibility = View.GONE
    }

    private fun setImagePreviewForVideo(videoData: Video) {
        if (isVerticalVideo) {
            binding.videoMediaImage.maxWidth = 300
        } else {
            binding.videoMediaImage.maxWidth = videoData.imageWidth
        }
        val requestOption = RequestOptions().centerCrop()
        Glide
            .with(binding.videoMediaImage.context)
            .load(videoData.imageURL)
            .apply(requestOption)
            .into(binding.videoMediaImage)
        binding.videoMediaImage.setOnClickListener {
            if (videoData.adconfig?.playVideoAds == true && videoActivityViewModel.isAdsContentContextualTargetingEnabled()) {
                observeAdsContentUiStateEvents(videoData)
                videoActivityViewModel.dispatchVideoClickEvent(videoData.id)
            } else {
                bindUserInitiatedVideo(videoData, null)
            }
        }
        binding.videoMediaImage.bringToFront()
    }

    private fun getAspectRatio(item: Video): Float {
        val imageWidth = item.imageWidth
        val imageHeight = item.imageHeight
        return if (imageHeight != null && imageWidth > 0 && imageHeight > 0) {
            imageWidth / imageHeight.toFloat()
        } else {
            // fallback if some of the sizes in unknown
            if (isVerticalVideo) {
                9 / 16f
            } else {
                16 / 9f
            }
        }
    }

    private fun setVideoCaption(contentItem: Video) {
        val caption = contentItem.fullcaption
        if (caption != null) {
            binding.videoCaption.visibility = View.VISIBLE
            val sb = SpannableStringBuilder(caption)
            sb.setSpan(
                WpTextAppearanceSpan(
                    itemView.context,
                    VideoStyleHelper.getVideoCaptionStyle(binding.root.context),
                ),
                0,
                caption.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            sb.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                caption.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            binding.videoCaption.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    com.washingtonpost.android.articles.R.color.articles_main_text_color,
                ),
            )
            if (isVerticalVideo) {
                binding.videoCaption.gravity = Gravity.CENTER_HORIZONTAL
            } else {
                binding.videoCaption.gravity = Gravity.LEFT
            }
            binding.videoCaption.text = sb
        } else {
            binding.videoCaption.visibility = View.GONE
        }
    }

    private fun getVideoId(item: Video): String? =
        when (item.host) {
            Host.YOUTUBE -> Uri.parse(item.mediaURL).getQueryParameter("v")
            else -> item.getStreamUrl(
                videoActivityViewModel.pageConfig.videoMaxBitRateMobile,
                videoActivityViewModel.pageConfig.videoMaxBitRateTablet
            ) ?: item.streamURL ?: item.mediaURL
        }

    private fun isAutoplayVideo(): Boolean {
        val player2Manager = videoManager2.getPlayerManager(videoId)
        return (player2Manager?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_AUTOPLAY)
    }

    private fun isAutoplayLoopingVideo(): Boolean {
        val player2Manager = videoManager2.getPlayerManager(videoId)
        return isAutoplayVideo() || player2Manager?.video?.playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL_MUTED
    }

    /**
     * Returns current video playback position if it is playing
     */
    private fun getPlaybackPosition(playType: Int): Long {
        var playbackPosition: Long = -1
        if (playType == com.wapo.flagship.features.posttv.model.Video.PLAY_TYPE_NORMAL) {
            val player2Manager = videoManager2.getPlayerManager(videoId)
            if (player2Manager?.isPlaying() == true) {
                playbackPosition = player2Manager.getPlaybackPosition() ?: playbackPosition
            }
        }
        return playbackPosition
    }

    private fun observeVideoLoadedEvent(item: Video) {
        videosLoadedEventObserver?.let {
            videoActivityViewModel.videoLoadedEvent.removeObserver(it)
        }
        videosLoadedEventObserver =
            Observer<Pair<String, String>> { pair ->
                if (pair.first == articleModel.contenturl && pair.second == item.id) {
                    val playerFrame: FrameLayout? = videoManager2.getPlayerFrameContainer(videoId)
                    if (playerFrame == null) {
                        displayVideoIfRequired(item)
                    }
                }
            }.also {
                videoActivityViewModel.videoLoadedEvent.observeForever(it)
            }
    }

    private fun showProgressBar() {
        binding.progressBar.apply {
            visibility = View.VISIBLE
            bringToFront()
        }
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }
}

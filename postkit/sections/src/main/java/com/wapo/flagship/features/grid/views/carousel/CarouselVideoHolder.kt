/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views.carousel

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginBottom
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils.isConnectedOrConnecting
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.ViewUtil.requireActivity
import com.wapo.android.commons.util.formatTimeSeconds
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.CarouselVideo
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.PostTvPlayer2Coordinator
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager
import com.wapo.flagship.features.posttv.VideoTracker2
import com.wapo.flagship.features.posttv.model.PlaybackState
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.sections.SectionActivity
import com.washingtonpost.android.recirculation.carousel.adapter.CarouselRecyclerViewAdapter
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselArrowsClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselVideoViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.sections.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class CarouselVideoHolder(itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup) : GridViewHolder(itemView) {

    private val tag = CarouselVideoHolder::class.java.simpleName
    private var carouselView: CarouselView? = null
    private val errorContainer = itemView.findViewById<ViewGroup>(R.id.error_container)
    private var activeVH: CarouselRecyclerViewAdapter.MediaViewHolder? = null
    private var videoHeadline: TextView? = null
    private var showHeadlineOnVideo = AtomicBoolean()
    private val headlineDelayHandler = Handler(Looper.getMainLooper()) {
        videoHeadline?.visibility = View.GONE
        true
    }
    private var carouselVideo: CarouselVideo? = null
    private var playbackStateObserver: Observer<PlaybackState>? = null
    private var player2Manager: PostTvPlayer2Manager? = null
    private var previousPosition: Int = 0

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        Logger.d(tag, "CarouselVideo, bind(), this=${this.hashCode()}, position=$position")
        val carouselVideo = gridAdapter.items[position] as? CarouselVideo
        carouselVideo ?: return

        // Initialize PostTvPlayer2Manager
        player2Manager = PostTvPlayer2Coordinator.getOrCreatePlayer(carouselVideo.id ?: "$tag$position", itemView.context.requireActivity())
        player2Manager?.apply {
            mute()
            val containerResId = itemView.findActivityOfType<SectionActivity>()?.player2ContainerResId
            if (containerResId != null) updatePlayerContainerView(containerResId)
            videoHeadline = getPlayerContainerView()?.findViewById<TextView>(com.washingtonpost.android.recirculation.R.id.player_headline)?.apply {
                typeface = ResourcesCompat.getFont(itemView.context, com.wapo.view.R.font.wp_postoniwide_font_family)
            }
            showController()
            setRespectAudioFocus(false)
        }

        this.carouselVideo = carouselVideo
        carouselView = itemView.findViewById(R.id.carousel_view)
        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                gridAdapter.onCarouselVideoCardClicked?.invoke(carouselVideo.items.map { it.video }, positionInCarousel)
            }
        }

        val cardWidth = itemView.resources.getDimensionPixelSize(R.dimen.carousel_video_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_video_card_padding)
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val isFullWidth = carouselVideo?.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val sideMargin = if (isSmallerBreakPoint) parent.context.resources.getDimension(R.dimen.card_grid_item_padding).toInt() else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        val sidePadding = if (isSmallerBreakPoint) ((parent.context.resources.getDimension(R.dimen.card_grid_item_padding).toInt()) * 2) else 0
        val cardAspectRatio = (carouselVideo.items.minOfOrNull { it.media.aspectRatio }) ?: 0f
        val cardHeight = cardWidth / cardAspectRatio
        carouselView?.cardHeight = cardHeight.toInt()
        carouselView?.initLayout(requestListener, clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean { return true }
            },
            shouldDisplaySectionName = false,
            shouldDisplayArrow = !isSmallerBreakPoint,
            shouldDisplayDateTime = false,
            fixedCardWidth = cardWidth,
            fixedCardPadding = cardPadding,
            marginStart = sideMargin,
            marginEnd = sideMargin,
            onEllipsisClick = { actionItem ->
                gridAdapter.onEllipsisClick?.invoke(
                    actionItem.toEllipsisActionItem(menuType = EllipsisMenu.Carousel)
                )
            }
        )
        carouselView?.recyclerView?.apply {
            setPadding(sidePadding, paddingTop, sidePadding, paddingBottom)
            addOnScrollListener(ScrollListener(gridAdapter))
        }
        carouselView?.setOnCarouselArrowsClickedListener(object: OnCarouselArrowsClickedListener {
            override fun onForwardArrowClicked() {
                carouselView?.postDelayed( { gridAdapter.onVideoCarouselStateIdle(this@CarouselVideoHolder) }, 100)
            }

            override fun onBackwardArrowClicked() {
                carouselView?.postDelayed( { gridAdapter.onVideoCarouselStateIdle(this@CarouselVideoHolder) }, 100)
            }
        })
        carouselView?.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })

        // Set Data to CarouselView
        val carouselViewItems = carouselVideo.items.mapIndexed { index, item ->
            val video = item.video
            CarouselVideoViewItem(item.media.link?.url ?: "", item.media.url, formatTimeSeconds(video.duration), video.headline, item.media.altText ?: "")
        }

        carouselView?.carouselItemsFetchListener?.onCarouselItemsFetched(carouselViewItems)
        observePlaybackStateEvents()

        // Caching video data with ExoPlayerCache.
        val activity = itemView.findActivityOfType<SectionActivity>()
        if (activity?.isDataUsageRestricted == false) {
            carouselVideo.items.forEach {
                if (it.video.id.isEmpty()) return@forEach
                ExoPlayerCache.cacheVideo(it.video.id)
            }
        }
    }

    fun onScrollStateIdle(adapter: GridAdapter) {
        Logger.d(tag, "CarouselVideo, onScrollStateIdle(), this=${this.hashCode()}")
        playFirstVisibleItem(adapter)
    }

    override fun unbind() {
        Logger.d(tag, "CarouselVideo, unbind(), this=${this.hashCode()}")
        release()
        playbackStateObserver?.let { player2Manager?.playbackState?.removeObserver(it) }
        playbackStateObserver = null
        player2Manager?.releasePlayer()
        player2Manager = null
        carouselView = null
    }

    fun release() {
        hideError()
        headlineDelayHandler.removeMessages(0)
        if (activeVH != null) {
            player2Manager?.stopMedia()
            activeVH?.release()
            activeVH = null
        }
    }

    private fun playFirstVisibleItem(adapter: GridAdapter) {
        Logger.d(tag, "CarouselVideo, playFirstVisibleItem(), this=${this.hashCode()}")
        val activity = itemView.findActivityOfType<SectionActivity>()

        if (activity?.canAutoPlayCarouselVideo() == false) {
            release()
            return
        }

        val rv = carouselView?.recyclerView
        val llm = rv?.layoutManager as? LinearLayoutManager ?: return

        val firstPosition = llm.findFirstCompletelyVisibleItemPosition()

        if (firstPosition != RecyclerView.NO_POSITION) {
            val vh = rv.findViewHolderForAdapterPosition(firstPosition)
            if (vh is CarouselRecyclerViewAdapter.MediaViewHolder) {
                if (activeVH != null) {
                    if (activeVH != vh) {
                        activeVH?.release()
                    } else {
                        return
                    }
                }
                hideError()
                val video = carouselVideo?.items?.get(firstPosition)?.video ?: return
                val carouselVideo = video.copy(adTagUrl = null)

                vh.getVideoContainer()?.let { container ->
                    vh.focus {
                        adapter.onCarouselVideoStartsPlaying(this)
                        player2Manager?.apply {
                            addPlayerContainerViewToItemView(container)
                            updateHeadline(video)
                            showHeadlineOnVideo.set(true)
                            val swipeDirection = getSwipeDirection(firstPosition, previousPosition)
                            val tracking = VideoTracker2.Tracking(
                                firstPosition + 1,
                                VideoTracker2.VideoType.VERTICAL_CAROUSEL,
                                video,
                                swipeDirection
                            )
                            playMedia(carouselVideo , true, tracking)
                        }
                    }
                }
                activeVH = vh
            }
        }
        previousPosition = firstPosition
    }

    private fun getSwipeDirection(firstPosition: Int, previousPosition: Int): VideoTracker2.SwipeDirection {
        return if (firstPosition == previousPosition) {
            VideoTracker2.SwipeDirection.NONE
        } else if (firstPosition > previousPosition) {
            VideoTracker2.SwipeDirection.FORWARD
        } else {
            VideoTracker2.SwipeDirection.BACK
        }
    }

    private fun observePlaybackStateEvents() {
        playbackStateObserver = Observer<PlaybackState> {
            Logger.d(tag, "CarouselVideo, observePlaybackStateEvents(), this=${this.hashCode()}, state=$it")
            when (it) {
                PlaybackState.Buffering -> {}
                PlaybackState.Ended -> { release() }
                is PlaybackState.Error -> {
                    val canShowError = activeVH != null
                    release()
                    if (canShowError) showError()
                }
                PlaybackState.Idle -> {}
                PlaybackState.Ready -> {
                    activeVH?.prepare()
                    if (showHeadlineOnVideo.get()) {
                        showHeadlineOnVideo.set(false)
                        headlineDelayHandler.sendEmptyMessageDelayed(0, 2000)
                    }
                }
                PlaybackState.TrackChanged -> {}
                else -> {
                    // no op
                }
            }
        }.also {
            player2Manager?.playbackState?.observe(ProcessLifecycleOwner.get(), it)
        }
    }

    private fun updateHeadline(video: Video) {
        headlineDelayHandler.removeMessages(0)
        videoHeadline?.apply {
            text = video.headline
            visibility = View.VISIBLE
        }
    }

    private fun showError() {
        val isNetworkError = !isConnectedOrConnecting(itemView.context)
        val errorTextId = if (isNetworkError) R.string.vertical_video_error_offline else R.string.vertical_video_error_other
        val builder = SpannableStringBuilder()
        builder.append("Error: ")
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, builder.length, 0)
        builder.append(itemView.context.resources.getText(errorTextId))

        var errorTextView = errorContainer.findViewById(R.id.tv_vertical_video_error) as? TextView
        if (errorTextView == null) {
            val errorView = LayoutInflater.from(itemView.context).inflate(R.layout.carousel_video_error_view, null)
            errorContainer.addView(errorView)
            errorTextView = errorContainer.findViewById(R.id.tv_vertical_video_error) as? TextView
        }
        errorTextView?.text = builder

        val bottomMarin = itemView.context.resources.getDimension(R.dimen.vertical_videos_error_text_vertical_margin).toInt()
        errorContainer.visibility = View.VISIBLE
        errorContainer.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                errorContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                errorContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(errorContainer.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                animateViewHeight(errorContainer, 150, errorContainer.measuredHeight, bottomMarin)
            }
        })
    }

    private fun hideError() {
        animateViewHeight(errorContainer, 50, 0, 0)
    }

    private fun animateViewHeight(v: View, duration: Int, targetHeight: Int, targetMargin: Int) {
        val prevHeight = v.height
        val prevMargin = v.marginBottom
        val layoutParams = v.layoutParams as? ViewGroup.MarginLayoutParams
        v.visibility = View.VISIBLE
        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight)
        valueAnimator.addUpdateListener { animation ->
            v.layoutParams.height = animation.animatedValue as Int
            layoutParams?.bottomMargin = if (targetHeight - prevHeight > 0)
                (abs(targetMargin - prevMargin) * animation.animatedValue as Int) / abs(targetHeight - prevHeight)
            else 0
            v.requestLayout()
        }
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = duration.toLong()
        valueAnimator.start()
    }

    private inner class ScrollListener(val adapter: GridAdapter) : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                Logger.d(tag, "CarouselVideo, onScrollStateChanged(), this=${this.hashCode()}")
                adapter.onVideoCarouselStateIdle(this@CarouselVideoHolder)
            }
        }
    }
}

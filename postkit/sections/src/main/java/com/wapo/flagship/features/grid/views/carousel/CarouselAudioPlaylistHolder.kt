/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.setVisible
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.fragments.AudioPagerFragment
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.grid.*
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.*
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.viewmodels.SectionAudioPageState
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselAudioViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EmptyCardItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.sections.R


class CarouselAudioPlaylistHolder(itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup) : GridViewHolder(itemView) {

    private lateinit var carouselView: CarouselView
    private lateinit var carouselAudioPlaylist: CarouselAudioPlaylist
    private lateinit var rightArrow : ImageView
    private var gridAdapter:GridAdapter? = null
    private var pageStateObserver: Observer<SectionAudioPageState>? = null
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private var playListObserver: Observer<List<Playlist>?>? = null
    private var previousVisibleOnScreenState: Boolean = false
    private lateinit var ctaView: CompoundLabelView
    val activity = itemView.findActivityOfType<SectionActivity>()
    private val audioMediaActivityViewModel = activity?.sectionAudioMediaActivityViewModel
    private val carouselAudioMediaActivityViewModel = activity?.carouselAudioMediaActivityViewModel
    private val playListViewModel = activity?.playListViewModel

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        carouselAudioPlaylist = (gridAdapter.items[position] as? CarouselAudioPlaylist)!!
        carouselView = itemView.findViewById(R.id.carousel_view)
        ctaView = itemView.findViewById(R.id.cta_view)
        this.gridAdapter = gridAdapter
        if (carouselAudioPlaylist.cta == null) {
            ctaView.setVisible(false)
        } else {
            ctaView.setLabel(carouselAudioPlaylist.cta)
            ctaView.setVisible(true)
            ctaView.setOnClickListener {
                itemView.context?.let {
                    activity.apply {
                        val podcastPagerFragment = AudioPagerFragment.newInstance()
                        val supportFragmentManager = itemView.context.findActivityOfType<AppCompatActivity>()?.supportFragmentManager
                        if (supportFragmentManager != null) {
                            podcastPagerFragment.show(
                                supportFragmentManager,
                                AudioPagerFragment.FRAGMENT_TAG
                            )
                        }
                        val playlistItems = playListViewModel?.playlist?.value
                        if (playlistItems != null) {
                            gridAdapter.onCarouselAudioPlaylistArticleCardClicked?.invoke(carouselAudioPlaylist, playlistItems, 0)
                        }
                    }
                    playListViewModel?._playlistCtaClickEvent?.value = true
                    playListViewModel?._playlistClickTrackEvent?.value = true
                }
            }
        }

        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {

                val playlistItems = playListViewModel?.playlist?.value ?: return
                playlistItems.get(positionInCarousel).tracker?.isAudioPlaylist = true
                gridAdapter.onCarouselAudioPlaylistArticleCardClicked?.invoke(carouselAudioPlaylist, playlistItems, positionInCarousel)

                // Scroll to the selected audio
                carouselView.recyclerView.smoothScrollToPosition(positionInCarousel)
            }
        }


        val cardWidth = itemView.resources.getDimensionPixelSize(R.dimen.carousel_audio_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_video_card_padding)
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val isFullWidth = carouselAudioPlaylist?.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        carouselView.initLayout(
            requestListener,
            clickListener,
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
                val playlist = playListViewModel?.playlist?.value?.find { it.id == actionItem.url }
                gridAdapter.onEllipsisClick?.invoke(
                    actionItem.toEllipsisActionItem(menuType = EllipsisMenu.Carousel).copy(playlist = playlist)
                )
            }
        )

        carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })

        if (isSmallerBreakPoint) {
            val horizontalPadding =
                parent.context.resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding)
            if (horizontalPadding > 0) {
                carouselView.recyclerView?.setPadding(
                    horizontalPadding.toInt(),
                    0,
                    horizontalPadding.toInt(),
                    0
                )
                ctaView.setPadding(horizontalPadding.toInt(), itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), horizontalPadding.toInt(), 0)
            }
        } else {
            ctaView.setPadding(sideMargin, itemView.resources.getDimensionPixelSize(R.dimen.card_carousel_label_top_padding), sideMargin, 0)
        }
        observePageStateObserver()
        observeNowPlayingEventObserver()
        observePlayList()
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        carouselView.onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    private fun observePlayList() {
        playListObserver?.let { playListViewModel?.playlist?.removeObserver(it) }
        playListObserver = Observer { items ->
            val carouselItems = mapPlayListToCarouselItems(items)
            carouselView.carouselItemsFetchListener.onCarouselItemsFetched(carouselItems)
            ctaView.setVisible(!items.isNullOrEmpty())
        }
        playListViewModel?.playlist?.observeForever(playListObserver!!)
    }

    private fun mapPlayListToCarouselItems(items: List<Playlist>?) : List<CarouselViewItem> {
        if (items.isNullOrEmpty()) {
            return listOf(
                EmptyCardItem(
                    text1 = "No saved audio",
                    text2 = "Use the “add to playlist” icon to save audio articles and podcasts to listen later.",
                    iconRes = com.wapo.flagship.features.audio.R.drawable.ic_playlist_add_round)
            )
        }
        return items.map {
            CarouselAudioViewItem(
                carouseId = carouselAudioPlaylist.id,
                audioMediaConfigId = it.manifestUrl ?: it.id,
                headline = it.title.orEmpty(),
                artUrl = it.imageUrl,
                artAspectRatio = null,
                duration = null,
                articleUrl = it.contentUrl ?: it.id,
                audioArticleDisplayDate = null,
                audioArticleDisplayLabel = null,
                audioArticleDisplayTransparency = null,
                audioArticleTitlePrefix = null,
                audioArticleTitle = null,
                mediaUrl = it.mediaId,
                pageName = null,
                articleLinkIsWebType = false,
                overlayText = null,
                overlayPrefixImageUrl = null,
                carouselAudioPlaylist.cardify,
                carouselItemType = null
            )
        }
    }

    private fun observePageStateObserver() {
        pageStateObserver?.let {
            audioMediaActivityViewModel?.pageState?.removeObserver(it)
        }
        pageStateObserver = Observer<SectionAudioPageState> {
            scrollToPositionIfRequired(it.playListId, it.position)
        }.also {
            audioMediaActivityViewModel?.pageState?.apply {
                value?.let { scrollToPositionIfRequired(it.playListId, it.position) }
                observeForever(it)
            }
        }
    }

    private fun observeNowPlayingEventObserver() {
        nowPlayingObserver?.let {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = Observer<NowPlayingAudioItem?> {
            carouselAudioMediaActivityViewModel?.dispatchNowPlayingItem(it)
        }.also {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.apply { observeForever(it) }
        }
    }

    private fun scrollToPositionIfRequired(playListId: String?, position: Int) {
        if (carouselAudioPlaylist?.id != playListId)
            return

        val firstFullyVisibleItem = (carouselView.recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        val lastFullyVisibleItem = (carouselView.recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

        if (position < firstFullyVisibleItem || position > lastFullyVisibleItem) {
            val targetPosition = when {
                /* By default, smooth scrolling just brings the item into view,
                 * so on tablet we want to go one more to get the item centered in the carousel */
                AppContextUtils.isTablet() && position < firstFullyVisibleItem -> position - 1
                AppContextUtils.isTablet() && position > lastFullyVisibleItem -> position + 1
                /* Since phones only show one fully visible item,
                 * we can just scroll to that position directly */
                else -> position
            }
            val itemCount = carouselView.recyclerView.adapter?.itemCount ?: 0
            if (targetPosition in 0 until itemCount) {
                carouselView.recyclerView.smoothScrollToPosition(targetPosition)
            }
        }
    }


    fun onScrollStateIdle(visibleOnScreen: Boolean) {
        if (visibleOnScreen == previousVisibleOnScreenState) {
            return
        }
        previousVisibleOnScreenState = visibleOnScreen
        audioMediaActivityViewModel?.nowPlayingAudioItem?.value?.let {
            // No need to scroll if state is already stopped.
            if (it.audioPlaybackState == AudioPlaybackState.Cleared) return@let
            val playListId = it.playlistId
            val position = it.nowPlayingItemIndex
            if (playListId != null && position > -1) {
                scrollToPositionIfRequired(playListId, position)
            }
        }
    }

    companion object {
        val TAG = CarouselAudioPlaylistHolder::class.toString()
    }

    override fun unbind() {
        super.unbind()
        this.gridAdapter = null
        pageStateObserver?.let {
            audioMediaActivityViewModel?.pageState?.removeObserver(it)
        }
        pageStateObserver = null
        nowPlayingObserver?.let {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        nowPlayingObserver = null
        playListObserver?.let { playListViewModel?.playlist?.removeObserver(it) }
        playListObserver = null
    }
}
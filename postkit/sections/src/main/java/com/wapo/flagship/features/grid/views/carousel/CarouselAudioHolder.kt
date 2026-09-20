/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.getUrlWithoutParameters
import com.wapo.android.commons.util.toDateLong
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.extentions.launchWhenAttached
import com.wapo.flagship.features.grid.GridActivity
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.ItemType
import com.wapo.flagship.features.grid.VoiceUtils
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.extensions.toEllipsisActionItem
import com.wapo.flagship.features.grid.model.CarouselAudio
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.CarouselMapper
import com.wapo.flagship.features.grid.model.EllipsisMenu
import com.wapo.flagship.features.grid.model.LinkType
import com.wapo.flagship.features.grid.views.carousel.tracking.CarouselTrackingHelper
import com.wapo.flagship.features.personalizedpodcasts.model.PersonalizedPodcast
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.tracking.SectionTrackerFactory
import com.wapo.flagship.features.sections.tracking.SectionsTracker
import com.wapo.flagship.features.sections.viewmodels.SectionAudioPageState
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.displayDate
import com.wapo.text.TextMeasure
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselAudioViewItem
import com.washingtonpost.android.recirculation.carousel.views.CarouselView
import com.washingtonpost.android.recirculation.databinding.CarouselAudioItemBinding
import com.washingtonpost.android.sections.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

class CarouselAudioHolder(itemView: View, val requestListener: CarouselProvider, val parent: ViewGroup) : GridViewHolder(itemView) {

    private lateinit var carouselView: CarouselView
    private var carouselAudio: CarouselAudio? = null
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private var carouselAudioId: String? = null
    private var carouselTrackingHelper: CarouselTrackingHelper? = null
    private var gridAdapter: GridAdapter? = null
    private var pageStateObserver: Observer<SectionAudioPageState>? = null
    private var nowPlayingObserver: Observer<NowPlayingAudioItem?>? = null
    private var job: Job? = null
    private var previousVisibleOnScreenState: Boolean = false
    val activity = itemView.findActivityOfType<SectionActivity>()
    private val gridActivity = itemView.findActivityOfType<GridActivity>()
    private val isLoggedInUser = gridActivity?.getGridEnvironment()?.isLoggedInUser() ?: false
    private val audioMediaActivityViewModel = activity?.sectionAudioMediaActivityViewModel
    private val carouselAudioMediaActivityViewModel = activity?.carouselAudioMediaActivityViewModel
    private val playListViewModel = activity?.playListViewModel
    private val personalizedPodcastViewModel = activity?.persoPodcastViewModel

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val initialCarouselData = gridAdapter.items[position] as? CarouselAudio
        this.carouselAudio = initialCarouselData
        this.carouselAudioId = initialCarouselData?.id
        carouselView = itemView.findViewById(R.id.carousel_view)
        this.gridAdapter = gridAdapter

        observePageStateObserver()
        observeNowPlayingEventObserver()
        if (initialCarouselData?.carouselType == ItemType.CAROUSEL_PERSONALIZED_PODCAST) {
            initialCarouselData.cardify?.let { observePersoPodcast(it, true) }
        }

        val clickListener: OnCarouselClickedListener = object : OnCarouselClickedListener {
            override fun onCardClicked(url: String?, positionInCarousel: Int) {
                    carouselAudio?.let {
                        gridAdapter.onCarouselAudioArticleCardClicked?.invoke(
                            it.copy(
                                items = it.items.filter { item ->
                                    item.audio != null || item.audioArticle != null
                                }
                            ),
                            positionInCarousel
                        )
                    }
                // Scroll to the selected audio
                carouselView.recyclerView.smoothScrollToPosition(positionInCarousel)
            }
        }

        val cardWidth = itemView.resources.getDimensionPixelSize(R.dimen.carousel_audio_card_width)
        val cardPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_video_card_padding)
        val arrowPadding = itemView.resources.getDimensionPixelSize(R.dimen.carousel_arrow_padding)
        val isFullWidth = initialCarouselData?.resolvedColumnSpan == (parent as? WPGridView)?.getColumnCount()
        val isSmallerBreakPoint = (parent as? WPGridView)?.getColumnCount() == 1
        val sideMargin = if (isSmallerBreakPoint) 0 else if (isFullWidth) (parent as? WPGridView)?.getSideMargin() ?: 0 else arrowPadding
        val cardAspectRatio = (initialCarouselData?.items?.minOfOrNull { it.media?.aspectRatio ?: 1f }) ?: 1f
        val cardHeight = cardWidth / cardAspectRatio
        carouselView.cardHeight = cardHeight.toInt()
        carouselView.initLayout(requestListener, clickListener,
            object : CarouselView.CarouselConsumeTouchEventRule {
                override fun canCarouselConsumeTouchEvent(): Boolean { return false }
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
                val item = initialCarouselData?.items?.firstOrNull { it.link?.url == actionItem.url }
                val audioMediaConfig = gridAdapter.generateAudioMediaConfig?.invoke(item)
                gridAdapter.onEllipsisClick?.invoke(
                    actionItem.toEllipsisActionItem(menuType = EllipsisMenu.Carousel)
                        .copy(playlist = playlist, audioMediaConfig = audioMediaConfig)
                )
            },
            getNowPlayingAudioItem = {
                carouselAudioMediaActivityViewModel?.nowPlayingAudioItem?.value
            }
        )

        // Set Data to CarouselView
        val carouselViewItems = initialCarouselData?.items?.mapNotNull { item ->
            if (item.carouselItemType == PERSONALIZED_PODCAST_PLACEHOLDER) {
                observePersoPodcast(initialCarouselData.cardify == true, false, initialCarouselData, position )
                null
            } else {
                val audioConfig =
                    gridAdapter.generateAudioMediaConfig?.invoke(item) ?: return@mapNotNull null
                val audioMediaConfigId = audioConfig.id
                val art = item.media
                val headline = item.headline
                val link = item.link
                val dateLong = toDateLong(item.audio?.displayDate)
                val url =
                    VoiceUtils.getPreferredVoiceUrl(item.audioArticle) ?: item.audio?.streamUrl
                CarouselAudioViewItem(
                    initialCarouselData.id,
                    audioMediaConfigId,
                    headline,
                    art?.url,
                    art?.aspectRatio,
                    VoiceUtils.getPreferredVoiceDuration(item.audioArticle) ?: item.audio?.duration,
                    if (PersonalizedPodcastHelper.isPersonalizedPodcastItem(item.carouselItemType)) item.audio?.streamUrl?.getUrlWithoutParameters() ?: "" else link?.url ?: "",
                    displayDate(dateLong),
                    item.audioArticle?.label?.text,
                    item.audioArticle?.label?.secondaryText,
                    item.audioArticle?.titlePrefix,
                    item.audioArticle?.title,
                    url,
                    item.audioArticle?.tracking?.pageName,
                    link?.type == LinkType.WEB,
                    art?.overlay?.text,
                    art?.overlay?.prefixImage?.url,
                    initialCarouselData.cardify,
                    item.carouselItemType
                )
            }
        }

        calculateMinCardHeight(carouselViewItems)
        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(carouselViewItems)

        // Removing the existing listener if it exists to prevent duplicates
        scrollListener?.let {
            carouselView.recyclerView.removeOnScrollListener(it)
        }

        // Create new listener and saving reference
        scrollListener = ScrollListener(gridAdapter).also {
            carouselView.recyclerView.addOnScrollListener(it)
        }

        carouselView.setOnCarouselItemTouchListener(object: OnCarouselItemTouchListener {
            override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                itemView.findActivityOfType<GridActivity>()?.let { activity ->
                    val pagerView = activity.getGridEnvironment().getPager()
                    pagerView?.setShouldAllowScroll(!disallowIntercept)
                }
            }
        })

        if (carouselTrackingHelper == null) {
            initTracking()
        }
        if (isSmallerBreakPoint) {
            val horizontalPadding =
                parent.context.resources.getDimension(R.dimen.carousel_card_single_column_margin_plus_padding)
            if (horizontalPadding > 0) {
                carouselView.recyclerView.setPadding(
                    horizontalPadding.toInt(),
                    0,
                    horizontalPadding.toInt(),
                    0
                )
            }
        }
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        carouselView.onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    /**
     * calculate and set the minimumHeight of recyclerView.
     * Need to do so to prevent cards changing their height when clicking the play button
     */
    private fun calculateMinCardHeight(carouselViewItems: List<CarouselAudioViewItem>?) {
        var tallestHeadlineHeight = 0
        var tallestAudioItem: CarouselAudioViewItem? = null
        val cardWidth = itemView.resources.getDimensionPixelSize(R.dimen.carousel_audio_card_width)

        carouselViewItems?.forEach { item ->
            val measureTextHeight = TextMeasure().measureTextHeight(
                item.headline,
                cardWidth,
                itemView.context,
                com.washingtonpost.android.recirculation.R.style.audio_carousel_item_headline
            )
            if (measureTextHeight > tallestHeadlineHeight) {
                tallestHeadlineHeight = measureTextHeight
                tallestAudioItem = item
            }
        }

        tallestAudioItem?.let {
            val dummyView: CarouselAudioItemBinding =
                CarouselAudioItemBinding.inflate(LayoutInflater.from(itemView.context))
            val headlineView: TextView =
                dummyView.root.findViewById(com.washingtonpost.android.recirculation.R.id.item_headline)
            headlineView.text = it.headline

            dummyView.root.measure(
                View.MeasureSpec.makeMeasureSpec(cardWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            val tallestCardHeight = dummyView.root.measuredHeight


            val cardVerticalMargins = itemView.resources.getDimensionPixelSize(com.washingtonpost.android.recirculation.R.dimen.carousel_audio_card_vertical_margin) * 2
            carouselView.recyclerView.minimumHeight = tallestCardHeight + cardVerticalMargins
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

    private fun observePersoPodcast(
        cardify: Boolean,
        isPersoPodCarousel: Boolean,
        carouselAudioWithPlaceholder: CarouselAudio? = null,
        carouselPosition: Int? = null
    ) {
        personalizedPodcastViewModel?.getListOfPodcasts()
        job?.cancel()
        job = itemView.launchWhenAttached {
            personalizedPodcastViewModel?.uiState?.collectLatest { uiState ->
                val items = uiState.personalizedPodcasts
                if (items.isEmpty()) return@collectLatest

                val carouselItems = mapPodcastsToCarouselItems(items, cardify)
                val audioItems = items.mapNotNull { CarouselMapper.mapPersoPodToAudioItem(it) }

                if (audioItems.isEmpty()) return@collectLatest
                val firstAudioItem = audioItems.first()

                withContext(Dispatchers.Main) {
                    if (isPersoPodCarousel) {
                        carouselView.carouselItemsFetchListener.onCarouselItemsFetched(carouselItems)
                        carouselAudio = CarouselAudio(audioItems, cardify).apply {
                            id = carouselAudioId
                        }
                    }

                    if (isLoggedInUser) {
                        updateAudioCarousel(firstAudioItem, carouselAudioWithPlaceholder, carouselPosition)
                    }
                }
            }
        }
    }

    private fun updateAudioCarousel(firstAudioItem: CarouselAudioItem, carouselAudioWithPlaceholder: CarouselAudio?, carouselPosition: Int?) {
        carouselAudioWithPlaceholder?.run {
            val copyOfLatestPodcastItems = items.toMutableList()
            val replaceIndex = copyOfLatestPodcastItems.indexOfFirst {
                PersonalizedPodcastHelper.isPersonalizedPodcastItem(it.carouselItemType)
            }

            if (replaceIndex != -1) {
                val currentItem = copyOfLatestPodcastItems[replaceIndex]
                if (currentItem.audio?.mediaId != firstAudioItem.audio?.mediaId) {

                    copyOfLatestPodcastItems[replaceIndex] = firstAudioItem
                    items = copyOfLatestPodcastItems

                    carouselView.post {
                        if (carouselPosition != null) {
                            if (gridAdapter != null && carouselPosition < gridAdapter!!.itemCount) {
                                gridAdapter?.notifyItemChanged(carouselPosition)
                            }
                        }
                    }
                }
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
        if (carouselAudio?.id != playListId)
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

    private fun mapPodcastsToCarouselItems(items: List<PersonalizedPodcast>, cardify: Boolean) : List<CarouselAudioViewItem> {
        return items.map {
            val dateLong = toDateLong(it.createdAt)
            CarouselAudioViewItem(
                carouseId = carouselAudio?.id,
                audioMediaConfigId = it.audioFilePath,
                headline = it.title.orEmpty(),
                artUrl = it.image,
                artAspectRatio = null,
                duration = it.audioDuration?.toLong(),
                articleUrl = it.audioFilePath?.getUrlWithoutParameters() ?: "",
                audioArticleDisplayDate = displayDate(dateLong),
                audioArticleDisplayLabel = null,
                audioArticleDisplayTransparency = null,
                audioArticleTitlePrefix = null,
                audioArticleTitle = null,
                mediaUrl = it.audioFilePath,
                pageName = null,
                articleLinkIsWebType = false,
                overlayText = null,
                overlayPrefixImageUrl = null,
                cardify,
                carouselItemType = it.itemType
            )
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
        val TAG = CarouselAudioHolder::class.toString()
        const val NAVIGATION_AUDIO_CAROUSEL_FORWARD = "audio_carousel_forward"
        const val NAVIGATION_AUDIO_CAROUSEL_BACK = "audio_carousel_back"
        const val PERSONALIZED_PODCAST_PLACEHOLDER = "personalized_podcast_placeholder"
    }

    private inner class ScrollListener(val adapter: GridAdapter) : RecyclerView.OnScrollListener() {
        private val sectionTracker: SectionsTracker? = SectionTrackerFactory.get(itemView.context)
        private val llm = carouselView.recyclerView?.layoutManager as? LinearLayoutManager
        private var previousScrollPosition: Int = 1
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val newScrollPosition = llm?.findFirstCompletelyVisibleItemPosition()?.plus(1) ?: return
                val isAudioPlayerSheetVisible = activity?.isAudioPlayerSheetVisible ?: false
                if (newScrollPosition > previousScrollPosition && !isAudioPlayerSheetVisible) {
                    sectionTracker?.trackAudioCarouselNavigation(NAVIGATION_AUDIO_CAROUSEL_FORWARD)
                } else if (newScrollPosition < previousScrollPosition && !isAudioPlayerSheetVisible) {
                    sectionTracker?.trackAudioCarouselNavigation(NAVIGATION_AUDIO_CAROUSEL_BACK)
                }
                previousScrollPosition = newScrollPosition

                Logger.d(TAG, "CarouselAudio, onScrollStateChanged(), this=${this.hashCode()}")
            }
        }
    }


    private fun initTracking() {
        carouselTrackingHelper = object : CarouselTrackingHelper() {
            override fun trackCarouselSeen() {
                SectionTrackerFactory.get(itemView.context).trackAudioCarouselSeen(gridAdapter?.backToFront ?: false)
                gridAdapter?.backToFront = false
            }
        }
        carouselTrackingHelper?.bind(carouselView.recyclerView)
    }

    override fun unbind() {
        super.unbind()
        carouselTrackingHelper?.unbind(carouselView.recyclerView)
        carouselTrackingHelper = null
        // Cleaning up the scroll listener when the view is recycled or unbound
        scrollListener?.let {
            carouselView.recyclerView.removeOnScrollListener(it)
        }
        scrollListener = null
        this.gridAdapter = null
        pageStateObserver?.let {
            audioMediaActivityViewModel?.pageState?.removeObserver(it)
        }
        pageStateObserver = null
        nowPlayingObserver?.let {
            audioMediaActivityViewModel?.nowPlayingAudioItem?.removeObserver(it)
        }
        job?.cancel()
        nowPlayingObserver = null
    }
}

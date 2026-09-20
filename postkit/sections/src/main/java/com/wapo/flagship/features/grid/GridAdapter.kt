package com.wapo.flagship.features.grid

import android.content.Context
import android.text.TextUtils
import android.util.SparseArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.containsValue
import androidx.core.util.forEach
import androidx.core.view.forEach
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.wapo.adsinf.BannerAdView
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdSlotType
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.UiUtils
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.grid.events.ActionButtonEvent
import com.wapo.flagship.features.grid.model.Ad
import com.wapo.flagship.features.grid.model.Alignment
import com.wapo.flagship.features.grid.model.Bleed
import com.wapo.flagship.features.grid.model.Card
import com.wapo.flagship.features.grid.model.CardSegmentType
import com.wapo.flagship.features.grid.model.Carousel
import com.wapo.flagship.features.grid.model.CarouselAudio
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.CarouselAudioPlaylist
import com.wapo.flagship.features.grid.model.CarouselComments
import com.wapo.flagship.features.grid.model.CarouselExternal
import com.wapo.flagship.features.grid.model.CarouselSevenLive
import com.wapo.flagship.features.grid.model.CarouselImmersion
import com.wapo.flagship.features.grid.model.CarouselRecipe
import com.wapo.flagship.features.grid.model.CarouselVideo
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.ElectionsDelayMessage
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.GlobalBanner
import com.wapo.flagship.features.grid.model.GlobalBannerState
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.HabitTiles
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.InlineOffer
import com.wapo.flagship.features.grid.model.Item
import com.wapo.flagship.features.grid.model.LabelItem
import com.wapo.flagship.features.grid.model.Link
import com.wapo.flagship.features.grid.model.Media
import com.wapo.flagship.features.grid.model.MediaType
import com.wapo.flagship.features.grid.model.NewsprintTopCard
import com.wapo.flagship.features.grid.model.RelatedLinkItem
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.model.SectionTopper
import com.wapo.flagship.features.grid.model.Separator
import com.wapo.flagship.features.grid.model.SeparatorSize
import com.wapo.flagship.features.grid.model.Vote
import com.wapo.flagship.features.grid.views.CompoundLabelView
import com.wapo.flagship.features.grid.views.LiveImageContainerView
import com.wapo.flagship.features.grid.views.NewsprintTopCardViewHolder
import com.wapo.flagship.features.grid.views.SlideShowContainerView
import com.wapo.flagship.features.grid.views.carousel.CarouselAudioHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselAudioPlaylistHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselCommentsHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselExternalHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselSevenLiveHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselImmersionHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselRecipeHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselVideoHolder
import com.wapo.flagship.features.grid.views.carousel.CarouselViewHolder
import com.wapo.flagship.features.grid.views.carousel.HabitTilesHolder
import com.wapo.flagship.features.grid.views.carousel.SectionTopperHolder
import com.wapo.flagship.features.grid.views.carousel.StackViewHolder
import com.wapo.flagship.features.grid.views.electionsdelay.ElectionsDelayHolder
import com.wapo.flagship.features.grid.views.vote.VoteHolder
import com.wapo.flagship.features.inlineoffer.InlineOfferHomepageHolder
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.lowdatamodelbanner.ui.LowDataBannerView
import com.wapo.flagship.features.newsprint.NewsprintBadgeOverlay
import com.wapo.flagship.features.newsprint.NewsprintHelper
import com.wapo.flagship.features.newsprint.NewsprintViewModel
import com.wapo.flagship.features.pagebuilder.CellMediaView
import com.wapo.flagship.features.pagebuilder.RelatedLinksView
import com.wapo.flagship.features.posttv.ExoPlayerCache
import com.wapo.flagship.features.posttv.listeners.PostTvApplication
import com.wapo.flagship.features.posttv.model.Video
import com.wapo.flagship.features.sections.SectionActivity
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.flagship.features.sections.model.TargetingContentUIState
import com.wapo.flagship.features.sections.utils.JTidTracker
import com.wapo.flagship.features.sections.viewmodels.SectionVideoActivityViewModel
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.features.subscribebanner.utils.GlobalBannerViewStateHelper
import com.wapo.view.AsyncCell
import com.wapo.view.habittiles.Tile
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.sections.databinding.GlobalBannerBinding
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.userhistory.models.VideoConclusionState
import com.washingtonpost.userhistory.viewmodel.UserHistoryViewModel


const val VIEW_TYPE_LABEL = 0
const val VIEW_TYPE_STORY = 1
const val VIEW_TYPE_AD = 2
const val VIEW_TYPE_SEPARATOR = 3
const val VIEW_TYPE_VOTE_GUIDE = 4
const val VIEW_TYPE_ELECTIONS_DELAY_MESSAGE = 5
const val VIEW_TYPE_CAROUSEL = 6
const val VIEW_TYPE_STACK = 7
const val VIEW_TYPE_SUBSCRIBE_BANNER = 8
const val VIEW_TYPE_STORY_FULL_CARD = 9
const val VIEW_TYPE_STORY_TOP_CARD = 10
const val VIEW_TYPE_STORY_BOTTOM_CARD = 11
const val VIEW_TYPE_STORY_MIDDLE_CARD = 12
const val VIEW_TYPE_CAROUSEL_VIDEO = 13
const val VIEW_TYPE_LABEL_TOP_CARD = 14
const val VIEW_TYPE_LABEL_MIDDLE_CARD = 15
const val VIEW_TYPE_STORY_FULL_CARD_FULL_BLEED = 16
const val VIEW_TYPE_STORY_TOP_CARD_FULL_BLEED = 17
const val VIEW_TYPE_STORY_BOTTOM_CARD_FULL_BLEED = 18
const val VIEW_TYPE_STORY_MIDDLE_CARD_FULL_BLEED = 19
const val VIEW_TYPE_STORY_FULL_CARD_CONTAINER_BLEED = 20
const val VIEW_TYPE_STORY_TOP_CARD_CONTAINER_BLEED = 21
const val VIEW_TYPE_STORY_BOTTOM_CARD_CONTAINER_BLEED = 22
const val VIEW_TYPE_STORY_MIDDLE_CARD_CONTAINER_BLEED = 23
const val VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD = 24
const val VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD = 25
const val VIEW_TYPE_STACK_FULL_CARD = 26
const val VIEW_TYPE_STACK_BOTTOM_CARD = 27
const val VIEW_TYPE_SEPARATOR_MIDDLE_CARD = 28
const val VIEW_TYPE_CAROUSEL_AUDIO = 29
const val VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD = 30
const val VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD = 31
const val VIEW_TYPE_IMMERSION_CAROUSEL = 32
const val VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD = 33
const val VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD = 34
const val VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD = 35
const val VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD = 36
const val VIEW_TYPE_CAROUSEL_VIDEO_MIDDLE_CARD = 37
const val VIEW_TYPE_STACK_MIDDLE_CARD = 38
const val VIEW_TYPE_STACK_TOP_CARD = 39
const val VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD = 40
const val VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD = 41
const val VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD = 42
const val VIEW_TYPE_LABEL_BOTTOM_CARD = 43
const val VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST = 44
const val VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD = 45
const val VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_TOP_CARD = 46
const val VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD = 47
const val VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD = 48
const val VIEW_TYPE_RECIPE_CAROUSEL = 49
const val VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD = 50
const val VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD = 51
const val VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD = 52
const val VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD = 53
const val VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED = 54
const val VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED = 55
const val VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED = 56
const val VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED = 57
const val VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED = 58
const val VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED = 59
const val VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED = 60
const val VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED = 61
const val VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD = 62
const val VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD = 63
const val VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD = 64
const val VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD = 65
const val VIEW_TYPE_STORY_WEB_EMBED = 66
const val VIEW_TYPE_HABIT_TILES = 67
const val VIEW_TYPE_HABIT_TILES_FULL_CARD = 68
const val VIEW_TYPE_HABIT_TILES_TOP_CARD = 69
const val VIEW_TYPE_HABIT_TILES_BOTTOM_CARD = 70
const val VIEW_TYPE_HABIT_TILES_MIDDLE_CARD = 71
const val VIEW_TYPE_NEWSPRINT_TOP_CARD = 72
const val VIEW_TYPE_LOW_DATA_BANNER = 73
const val VIEW_TYPE_COMMENTS_CAROUSEL = 74
const val VIEW_TYPE_COMMENTS_CAROUSEL_TOP_CARD = 75
const val VIEW_TYPE_COMMENTS_CAROUSEL_MIDDLE_CARD = 76
const val VIEW_TYPE_COMMENTS_CAROUSEL_BOTTOM_CARD = 77
const val VIEW_TYPE_COMMENTS_CAROUSEL_FULL_CARD = 78
const val VIEW_TYPE_SECTION_TOPPER = 79
const val VIEW_TYPE_SECTION_TOPPER_FULL_CARD = 80
const val VIEW_TYPE_SECTION_TOPPER_TOP_CARD = 81
const val VIEW_TYPE_SECTION_TOPPER_BOTTOM_CARD = 82
const val VIEW_TYPE_SECTION_TOPPER_MIDDLE_CARD = 83
const val VIEW_TYPE_INLINE_OFFER = 84
const val VIEW_TYPE_EXTERNAL_CAROUSEL = 85
const val VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD = 86
const val VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD = 87
const val VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD = 88
const val VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD = 89
const val VIEW_TYPE_EMPTY = 90
const val VIEW_TYPE_SEVEN_LIVE_CAROUSEL = 91
const val VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD = 92
const val VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD = 93
const val VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD = 94
const val VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD = 95

const val FRONT_HOMEPAGE = "front-homepage"


class GridAdapter(val context: Context) : RecyclerView.Adapter<GridViewHolder>() {

    internal var grid: Grid? = null
    internal lateinit var animatedImageLoader: AnimatedImageLoader
    internal lateinit var environment: GridEnvironment
    val items = mutableListOf<Item>()
    val cards = mutableListOf<Card>()
    private val adViews = SparseArray<View>()
    private val carouselViews = SparseArray<View>()
    private val carouselVideoViews = SparseArray<View>()
    private val carouselAudioViews = SparseArray<View>()
    private val carouselAudioPlaylistViews = SparseArray<View>()
    private val carouselImmersionViews = SparseArray<View>()
    private val carouselRecipeViews = SparseArray<View>()
    private val carouselCommentsViews = SparseArray<View>()
    private val carouselExternalViews = SparseArray<View>()
    private val carouselSevenLiveViews = SparseArray<View>()
    private val storyWebEmbedViews = SparseArray<View>()
    private val stackViews = SparseArray<View>()
    var onStoryViewClicked: ((HomepageStory, Int) -> Unit)? = null
    var onRelatedLinkClicked: ((HomepageStory, RelatedLinkItem) -> Unit)? = null
    var onLabelClicked: ((CompoundLabel, String?) -> Unit)? = null
    var onLiveBlogClicked: ((String, String?) -> Unit)? = null
    var onMediaClicked: ((HomepageStory, Link) -> Unit)? = null
    var onCarouselCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onCarouselImmersionCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onCarouselRecipeCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onStackCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onCarouselVideoCardClicked: ((List<Video>, Int) -> Unit)? = null
    var onCarouselAudioArticleCardClicked: ((CarouselAudio, Int) -> Unit)? = null
    var onCarouselAudioPlaylistArticleCardClicked: ((CarouselAudioPlaylist, List<Playlist>, Int) -> Unit)? =
        null
    var generateAudioMediaConfig: ((CarouselAudioItem?) -> AudioMediaConfig?)? = null
    var onBookmarkClick: ((EllipsisActionItem, ImageView, isStatusChecked: Boolean) -> Unit)? = null
    var onHabitTileClicked: ((Tile?) -> Unit)? = null
    var onNewsprintButtonClicked: (() -> Unit)? = null
    var onNewsprintSpanClicked: (() -> Unit)? = null
    var onStoryViewBlurbsLinkClicked: ((HomepageStory, String?) -> Unit)? = null
    var onCarouselCommentsCardClicked: ((Link?, Int) -> Unit)? = null
    var onCarouselExternalCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onCarouselSevenLiveCardClicked: ((List<Link>, Int) -> Unit)? = null
    var onImpressionEvent: ((BannerLifecycleEvent) -> Unit)? = null
    var onVerticalVideoClicked: (() -> Unit)? = null

    // Action Buttons functions
    var onActionButtonClicked: ((ActionButtonEvent) -> Unit)? = null

    // Lambda function for when Click Event needs to be propagated up to Activity.
    var onMessageBannerClicked: ((BannerEvent) -> Unit)? = null
    var onEllipsisClick: ((EllipsisActionItem) -> Unit)? = null

    var idList = mutableSetOf<String>()
    var breakFeaturesId = ArrayList<Set<String>>()
    var onSectionThresholdScrolled: ((Int, Int) -> Unit)? = null

    var onLowDataModeDisable: (() -> Unit)? = null

    // Hold last sub state for when section is refreshed.
    private var lastGlobalBannerState: GlobalBannerState = GlobalBannerState.Unknown

    // JTid is a time based identifier for section page view that will be send in ad requests as well.
    var jTid: Long? = null

    var isScreenXSmall: Boolean = false
    var backToFront: Boolean = false

    var newsprintViewModel: NewsprintViewModel? = null
    var sectionsHabitTilesRequestId: String? = null
    var sectionsHabitTilesTestGroup: String? = null
    var userHistoryViewModel: UserHistoryViewModel? = null
    var videoActivityViewModel: SectionVideoActivityViewModel? = null

    var onCarouselAudioPlaylistAdded: ((index: Int) -> Unit)? = null

    private var attachedRecyclerView: RecyclerView? = null
    private var nowPlayingAudioItem: NowPlayingAudioItem? = null

    init {
        setHasStableIds(true)
    }

    fun setGrid(grid: Grid?) {
        this.grid = grid
        items.clear()
        idList.clear()  // To clear the list of ids when the content re lodes or switch sections
    }

    /**
     * Called when the grid (data) has changed or the screen size has changed
     */
    fun addItems(
        grid: Grid,
        parent: RecyclerView,
        screenSizeLayout: ScreenSizeLayout,
        sectionDisplayName: String
    ) {
        // Releasing previous cache extensions when page is updated as data may change.
        releaseViewCacheExtensions(parent)
        items.clear()
        cards.clear()

        val isNewsprint = NewsprintHelper.isNewsprintSection(sectionDisplayName)
        isScreenXSmall = screenSizeLayout == ScreenSizeLayout.XSMALL
        val cardLayout = if (isScreenXSmall) grid.cards?.extraSmall else null
        var lastCardSegmentType = CardSegmentType.UNASSIGNED

        for (region in grid.regions) {
            region.items.forEachIndexed { chainIndex, chain ->
                CardificationUtils.assignCardType(chain, cardLayout, lastCardSegmentType)
                chain.items.forEachIndexed { tableIndex, table ->
                    CardificationUtils.assignCardType(table, cardLayout)
                    for (item in table.items) {
                        CardificationUtils.assignCardType(item, cardLayout, chain, table)
                        lastCardSegmentType = item.cardSegmentType
                        if (item.resolvedColumn != -1) {
                            if (item is Ad && environment.shouldSuppressAds()) {
                                continue
                            }
                            item.isNewsprint = isNewsprint
                            buildCard(item)
                            items.add(item)
                            if (item is CarouselAudioPlaylist) {
                                onCarouselAudioPlaylistAdded?.invoke(items.lastIndex)
                            }
                        }
                    }
                }
            }
        }

        // Update Global Banner to last state
        updateGlobalBannerItem(lastGlobalBannerState, false)
        val shouldSuppressAds = environment.shouldSuppressAds()
        if (!shouldSuppressAds) {
            updateAdItems()
        }
        findHomePageItems()
        parent.post {
            notifyDataSetChanged()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        attachedRecyclerView = recyclerView
    }

    override fun onViewAttachedToWindow(holder: GridViewHolder) {
        super.onViewAttachedToWindow(holder)

        onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    override fun onViewDetachedFromWindow(holder: GridViewHolder) {
        super.onViewDetachedFromWindow(holder)

        onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    /**
     * Associates card segments together into full Cards
     */
    private fun buildCard(item: Item) {
        when (item.cardSegmentType) {
            CardSegmentType.FULL_CARD,
            CardSegmentType.TOP_CARD -> {
                val card = Card(mutableListOf(item))
                cards.add(card)
                item.cardIndex = cards.lastIndex
                NewsprintHelper.handleNewsprintCard(item, true)
            }

            CardSegmentType.MIDDLE_CARD,
            CardSegmentType.BOTTOM_CARD -> {
                if (cards.isNotEmpty()) {
                    cards.last().items.add(item)
                    item.cardIndex = cards.lastIndex
                    NewsprintHelper.handleNewsprintCard(item, false)
                }
            }

            else -> {}
        }
    }

    /**
     * Update Global Banner Item in item list. Only notify RV if notifyChange value is true
     */
    fun updateGlobalBannerItem(globalBannerState: GlobalBannerState, notifyChange: Boolean = true) {
        lastGlobalBannerState = globalBannerState
        items.firstOrNull()?.let {
            if (it is GlobalBanner) {
                items[0] = GlobalBanner(globalBannerState)
                if (notifyChange) {
                    notifyItemChanged(0)
                }
            }
        }
    }

    fun removeInlineOfferItem() {
        val indices = items.withIndex()
            .filter { it.value is InlineOffer }
            .map { it.index }
        indices.reversed().forEach { index ->
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * Update Low Data Banner Item in item list. Only notify RV if notifyChange value is true
     */
    fun updateLowDataBannerItem(lowDataBanner: LowDataBanner, notifyChange: Boolean = true) {
        items.withIndex().firstOrNull { it.value is LowDataBanner }?.let { value ->
            items[value.index] = lowDataBanner
            if (notifyChange) {
                notifyItemChanged(value.index)
            }
        }
    }

    /**
     * Update Ad items in items list.
     */
    private fun updateAdItems() {
        // All ads are full bleed for fluid ads.
        items.filterIsInstance<Ad>().forEach { ad ->
            ad.forceFullBleed = true
        }
    }

    fun prepareViewCacheExtensions(parent: RecyclerView) {
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_AD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_VIDEO, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_VIDEO_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STACK, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STACK_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STACK_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STACK_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_IMMERSION_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_SEVEN_LIVE_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_RECIPE_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STACK_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(
            VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED,
            0
        )
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_STORY_WEB_EMBED, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_COMMENTS_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_COMMENTS_CAROUSEL_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_COMMENTS_CAROUSEL_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_COMMENTS_CAROUSEL_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_COMMENTS_CAROUSEL_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_EXTERNAL_CAROUSEL, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD, 0)
        parent.recycledViewPool.setMaxRecycledViews(VIEW_TYPE_INLINE_OFFER, 0)
        parent.setViewCacheExtension(object : RecyclerView.ViewCacheExtension() {
            override fun getViewForPositionAndType(
                recycler: RecyclerView.Recycler,
                position: Int,
                type: Int
            ): View? {
                return when (type) {
                    VIEW_TYPE_AD -> {
                        adViews[position]
                    }

                    VIEW_TYPE_CAROUSEL -> {
                        carouselViews[position]
                    }

                    VIEW_TYPE_CAROUSEL_VIDEO, VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD,
                    VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD -> {
                        carouselVideoViews[position]
                    }

                    VIEW_TYPE_STACK, VIEW_TYPE_STACK_FULL_CARD,
                    VIEW_TYPE_STACK_MIDDLE_CARD, VIEW_TYPE_STACK_BOTTOM_CARD, VIEW_TYPE_STACK_TOP_CARD -> {
                        stackViews[position]
                    }

                    VIEW_TYPE_CAROUSEL_AUDIO, VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD,
                    VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD, VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD -> {
                        carouselAudioViews[position]
                    }

                    VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD,
                    VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_TOP_CARD -> {
                        carouselAudioPlaylistViews[position]
                    }

                    VIEW_TYPE_IMMERSION_CAROUSEL, VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD,
                    VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD, VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD -> {
                        carouselImmersionViews[position]
                    }

                    VIEW_TYPE_RECIPE_CAROUSEL, VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD,
                    VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD, VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD -> {
                        carouselRecipeViews[position]
                    }

                    VIEW_TYPE_COMMENTS_CAROUSEL, VIEW_TYPE_COMMENTS_CAROUSEL_FULL_CARD,
                    VIEW_TYPE_COMMENTS_CAROUSEL_TOP_CARD, VIEW_TYPE_COMMENTS_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_COMMENTS_CAROUSEL_BOTTOM_CARD -> {
                        carouselCommentsViews[position]
                    }

                    VIEW_TYPE_EXTERNAL_CAROUSEL, VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD,
                    VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD, VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD -> {
                        carouselExternalViews[position]
                    }

                    VIEW_TYPE_SEVEN_LIVE_CAROUSEL, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD,
                    VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD -> {
                        carouselSevenLiveViews[position]
                    }

                    VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED,
                    VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD,
                    VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD,
                    VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD,
                    VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD,
                    VIEW_TYPE_STORY_WEB_EMBED -> {
                        storyWebEmbedViews[position]
                    }

                    else -> {
                        null
                    }
                }
            }
        })
    }

    fun refreshAds(parent: RecyclerView) {
        val positionList = mutableListOf<Int>()
        adViews.forEach { key, _ -> positionList.add(key) }
        for (i in 0 until adViews.size()) {
            val holder = (parent.getChildViewHolder(adViews[adViews.keyAt(i)]) as? AdViewHolder)
            holder?.unbind()
        }
        adViews.clear()
        positionList.forEach {
            notifyItemChanged(it)
        }
    }

    fun refreshHabitTiles(parent: RecyclerView) {
        parent.forEach {
            if (parent.getChildViewHolder(it) is HabitTilesHolder) {
                val adapterPosition = parent.getChildAdapterPosition(it)
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(adapterPosition)
                }
            }
        }
    }

    fun releaseViewCacheExtensions(parent: RecyclerView) {
        for (i in 0 until adViews.size()) {
            (parent.getChildViewHolder(adViews[adViews.keyAt(i)]) as? AdViewHolder)?.unbind()
        }
        adViews.clear()
        for (i in 0 until carouselViews.size()) {
            (parent.getChildViewHolder(carouselViews[carouselViews.keyAt(i)]) as? CarouselViewHolder)?.unbind()
        }
        carouselViews.clear()
        for (i in 0 until carouselVideoViews.size()) {
            (parent.getChildViewHolder(carouselVideoViews[carouselVideoViews.keyAt(i)]) as? CarouselVideoHolder)?.unbind()
        }
        carouselVideoViews.clear()
        for (i in 0 until carouselAudioViews.size()) {
            (parent.getChildViewHolder(carouselAudioViews[carouselAudioViews.keyAt(i)]) as? CarouselAudioHolder)?.unbind()
        }
        carouselAudioViews.clear()
        for (i in 0 until carouselAudioPlaylistViews.size()) {
            (parent.getChildViewHolder(carouselAudioPlaylistViews[carouselAudioPlaylistViews.keyAt(i)]) as? CarouselAudioPlaylistHolder)?.unbind()
        }
        carouselAudioPlaylistViews.clear()
        for (i in 0 until carouselImmersionViews.size()) {
            (parent.getChildViewHolder(carouselImmersionViews[carouselImmersionViews.keyAt(i)]) as? CarouselImmersionHolder)?.unbind()
        }
        carouselImmersionViews.clear()
        for (i in 0 until carouselRecipeViews.size()) {
            (parent.getChildViewHolder(carouselRecipeViews[carouselRecipeViews.keyAt(i)]) as? CarouselRecipeHolder)?.unbind()
        }
        carouselRecipeViews.clear()
        for (i in 0 until stackViews.size()) {
            (parent.getChildViewHolder(stackViews[stackViews.keyAt(i)]) as? StackViewHolder)?.unbind()
        }
        stackViews.clear()
        for (i in 0 until carouselCommentsViews.size()) {
            (parent.getChildViewHolder(carouselCommentsViews[carouselCommentsViews.keyAt(i)]) as? CarouselCommentsHolder)?.unbind()
        }
        carouselCommentsViews.clear()
        for (i in 0 until carouselExternalViews.size()) {
            (parent.getChildViewHolder(carouselExternalViews[carouselExternalViews.keyAt(i)]) as? CarouselExternalHolder)?.unbind()
        }
        carouselExternalViews.clear()
        for (i in 0 until carouselSevenLiveViews.size()) {
            (parent.getChildViewHolder(carouselSevenLiveViews[carouselSevenLiveViews.keyAt(i)]) as? CarouselSevenLiveHolder)?.unbind()
        }
        carouselSevenLiveViews.clear()
        for (i in 0 until storyWebEmbedViews.size()) {
            (parent.getChildViewHolder(storyWebEmbedViews[storyWebEmbedViews.keyAt(i)]) as? StoryViewHolder)?.unbind()
        }
        storyWebEmbedViews.clear()
    }

    fun releaseCallbackListeners() {
        onStoryViewClicked = null
        onRelatedLinkClicked = null
        onLabelClicked = null
        onLiveBlogClicked = null
        onMediaClicked = null
        onCarouselCardClicked = null
        onStackCardClicked = null
        onCarouselVideoCardClicked = null
        onMessageBannerClicked = null
        onActionButtonClicked = null
        onCarouselAudioArticleCardClicked = null
        onCarouselAudioPlaylistArticleCardClicked = null
        generateAudioMediaConfig = null
        onCarouselImmersionCardClicked = null
        onCarouselRecipeCardClicked = null
        onEllipsisClick = null
        onBookmarkClick = null
        onHabitTileClicked = null
        onStoryViewBlurbsLinkClicked = null
        onLowDataModeDisable = null
        onNewsprintButtonClicked = null
        onNewsprintSpanClicked = null
        onSectionThresholdScrolled = null
        onCarouselCommentsCardClicked = null
        onCarouselExternalCardClicked = null
        onCarouselSevenLiveCardClicked = null
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is LabelItem -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_LABEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_LABEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_LABEL_BOTTOM_CARD
                    else -> VIEW_TYPE_LABEL
                }

            }

            is HomepageStory -> {
                val bleed = items[position].bleed
                val hasWebEmbed = (items[position] as HomepageStory).webComponent?.url != null
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> {
                        when (bleed) {
                            Bleed.FULL -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED else VIEW_TYPE_STORY_FULL_CARD_FULL_BLEED
                            Bleed.CONTAINER -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED else VIEW_TYPE_STORY_FULL_CARD_CONTAINER_BLEED
                            else -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD else VIEW_TYPE_STORY_FULL_CARD
                        }
                    }

                    CardSegmentType.TOP_CARD -> {
                        when (bleed) {
                            Bleed.FULL -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED else VIEW_TYPE_STORY_TOP_CARD_FULL_BLEED
                            Bleed.CONTAINER -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED else VIEW_TYPE_STORY_TOP_CARD_CONTAINER_BLEED
                            else -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD else VIEW_TYPE_STORY_TOP_CARD
                        }
                    }

                    CardSegmentType.MIDDLE_CARD -> {
                        when (bleed) {
                            Bleed.FULL -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED else VIEW_TYPE_STORY_MIDDLE_CARD_FULL_BLEED
                            Bleed.CONTAINER -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED else VIEW_TYPE_STORY_MIDDLE_CARD_CONTAINER_BLEED
                            else -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD else VIEW_TYPE_STORY_MIDDLE_CARD
                        }
                    }

                    CardSegmentType.BOTTOM_CARD -> {
                        when (bleed) {
                            Bleed.FULL -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED else VIEW_TYPE_STORY_BOTTOM_CARD_FULL_BLEED
                            Bleed.CONTAINER -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED else VIEW_TYPE_STORY_BOTTOM_CARD_CONTAINER_BLEED
                            else -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD else VIEW_TYPE_STORY_BOTTOM_CARD
                        }
                    }

                    else -> if (hasWebEmbed) VIEW_TYPE_STORY_WEB_EMBED else VIEW_TYPE_STORY
                }
            }

            is Ad -> if (environment.shouldSuppressAds()) VIEW_TYPE_EMPTY else VIEW_TYPE_AD
            is Separator -> {
                if ((items[position] as Separator).insideCard) {
                    VIEW_TYPE_SEPARATOR_MIDDLE_CARD
                } else {
                    VIEW_TYPE_SEPARATOR
                }
            }

            is Vote -> VIEW_TYPE_VOTE_GUIDE
            is ElectionsDelayMessage -> VIEW_TYPE_ELECTIONS_DELAY_MESSAGE
            is Carousel -> if (environment.isPortraitPhone()) {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_STACK_FULL_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_STACK_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_STACK_BOTTOM_CARD
                    else -> VIEW_TYPE_STACK
                }
            } else {
                VIEW_TYPE_CAROUSEL
            }

            is GlobalBanner -> VIEW_TYPE_SUBSCRIBE_BANNER
            is LowDataBanner -> VIEW_TYPE_LOW_DATA_BANNER
            is InlineOffer -> VIEW_TYPE_INLINE_OFFER
            is CarouselVideo ->
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_CAROUSEL_VIDEO_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD
                    else -> VIEW_TYPE_CAROUSEL_VIDEO
                }

            is CarouselAudio ->
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD
                    else -> VIEW_TYPE_CAROUSEL_AUDIO
                }

            is CarouselAudioPlaylist ->
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD
                    else -> VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST
                }

            is CarouselImmersion -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD
                    else -> VIEW_TYPE_IMMERSION_CAROUSEL
                }
            }

            is CarouselRecipe -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD
                    else -> VIEW_TYPE_RECIPE_CAROUSEL
                }
            }

            is CarouselComments -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_COMMENTS_CAROUSEL_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_COMMENTS_CAROUSEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_COMMENTS_CAROUSEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_COMMENTS_CAROUSEL_BOTTOM_CARD
                    else -> VIEW_TYPE_COMMENTS_CAROUSEL
                }
            }

            is CarouselExternal -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD
                    else -> VIEW_TYPE_EXTERNAL_CAROUSEL
                }
            }

            is CarouselSevenLive -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD
                    else -> VIEW_TYPE_SEVEN_LIVE_CAROUSEL
                }
            }

            is HabitTiles -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_HABIT_TILES_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_HABIT_TILES_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_HABIT_TILES_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_HABIT_TILES_BOTTOM_CARD
                    else -> VIEW_TYPE_HABIT_TILES
                }
            }

            is NewsprintTopCard -> VIEW_TYPE_NEWSPRINT_TOP_CARD
            is SectionTopper -> {
                when (items[position].cardSegmentType) {
                    CardSegmentType.FULL_CARD -> VIEW_TYPE_SECTION_TOPPER_FULL_CARD
                    CardSegmentType.TOP_CARD -> VIEW_TYPE_SECTION_TOPPER_TOP_CARD
                    CardSegmentType.MIDDLE_CARD -> VIEW_TYPE_SECTION_TOPPER_MIDDLE_CARD
                    CardSegmentType.BOTTOM_CARD -> VIEW_TYPE_SECTION_TOPPER_BOTTOM_CARD
                    else -> VIEW_TYPE_SECTION_TOPPER
                }
            }

            else -> error("Wrong item type ${items[position]}")
        }
    }

    fun setImageLoader(animatedImageLoader: AnimatedImageLoader) {
        this.animatedImageLoader = animatedImageLoader
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val storyTypes = listOf(
            VIEW_TYPE_STORY,
            VIEW_TYPE_STORY_FULL_CARD,
            VIEW_TYPE_STORY_TOP_CARD,
            VIEW_TYPE_STORY_MIDDLE_CARD,
            VIEW_TYPE_STORY_BOTTOM_CARD,
            VIEW_TYPE_STORY_FULL_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_TOP_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_MIDDLE_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_BOTTOM_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_FULL_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_TOP_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_MIDDLE_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_BOTTOM_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD,
            VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD,
            VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD,
            VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD,
            VIEW_TYPE_STORY_WEB_EMBED
        )

        return when (viewType) {
            VIEW_TYPE_LABEL, VIEW_TYPE_LABEL_TOP_CARD, VIEW_TYPE_LABEL_MIDDLE_CARD, VIEW_TYPE_LABEL_BOTTOM_CARD -> onCreateLabelHolder(
                parent,
                viewType
            )

            in storyTypes -> onCreateStoryHolder(parent, viewType)
            VIEW_TYPE_AD -> onCreateAdHolder(parent)
            VIEW_TYPE_SEPARATOR, VIEW_TYPE_SEPARATOR_MIDDLE_CARD -> onCreateSeparatorHolder(
                parent,
                viewType
            )

            VIEW_TYPE_VOTE_GUIDE -> onCreateVoteHolder(parent)
            VIEW_TYPE_ELECTIONS_DELAY_MESSAGE -> onCreateElectionsDelayHolder(parent)
            VIEW_TYPE_CAROUSEL -> onCreateCarouselHolder(parent)
            VIEW_TYPE_STACK, VIEW_TYPE_STACK_BOTTOM_CARD, VIEW_TYPE_STACK_MIDDLE_CARD, VIEW_TYPE_STACK_FULL_CARD, VIEW_TYPE_STACK_TOP_CARD -> onCreateStackHolder(
                parent,
                viewType
            )

            VIEW_TYPE_SUBSCRIBE_BANNER -> onCreateGlobalBannerHolder(parent)
            VIEW_TYPE_LOW_DATA_BANNER -> onCreateLowDataModalBannerHolder(parent)
            VIEW_TYPE_INLINE_OFFER -> onCreateInlineOfferViewHolder(parent)
            VIEW_TYPE_CAROUSEL_VIDEO, VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD, VIEW_TYPE_CAROUSEL_VIDEO_MIDDLE_CARD, VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD -> onCreateCarouselVideoHolder(
                parent,
                viewType
            )

            VIEW_TYPE_CAROUSEL_AUDIO, VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD, VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD, VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD -> onCreateCarouselAudioHolder(
                parent,
                viewType
            )

            VIEW_TYPE_IMMERSION_CAROUSEL, VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD, VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD, VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD -> onCreateCarouselImmersionHolder(
                parent,
                viewType
            )

            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD, VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_TOP_CARD -> onCreateCarouselAudioPlaylistHolder(
                parent,
                viewType
            )

            VIEW_TYPE_RECIPE_CAROUSEL, VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD, VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD, VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD -> onCreateCarouselRecipeHolder(
                parent,
                viewType
            )

            VIEW_TYPE_COMMENTS_CAROUSEL, VIEW_TYPE_COMMENTS_CAROUSEL_FULL_CARD, VIEW_TYPE_COMMENTS_CAROUSEL_TOP_CARD, VIEW_TYPE_COMMENTS_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_COMMENTS_CAROUSEL_BOTTOM_CARD -> onCreateCarouselCommentsHolder(
                parent,
                viewType
            )

            VIEW_TYPE_EXTERNAL_CAROUSEL, VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD, VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD, VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD -> onCreateCarouselExternalHolder(
                parent,
                viewType
            )

            VIEW_TYPE_SEVEN_LIVE_CAROUSEL, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD, VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD -> onCreateCarouselSevenLiveHolder(
                parent,
                viewType
            )

            VIEW_TYPE_HABIT_TILES, VIEW_TYPE_HABIT_TILES_FULL_CARD, VIEW_TYPE_HABIT_TILES_TOP_CARD, VIEW_TYPE_HABIT_TILES_MIDDLE_CARD, VIEW_TYPE_HABIT_TILES_BOTTOM_CARD -> onCreateHabitTilesHolder(
                parent,
                viewType
            )

            VIEW_TYPE_NEWSPRINT_TOP_CARD -> onCreateNewsprintTopCardHolder(parent)
            VIEW_TYPE_SECTION_TOPPER, VIEW_TYPE_SECTION_TOPPER_FULL_CARD, VIEW_TYPE_SECTION_TOPPER_TOP_CARD, VIEW_TYPE_SECTION_TOPPER_MIDDLE_CARD, VIEW_TYPE_SECTION_TOPPER_BOTTOM_CARD -> onCreateSectionTopperHolder(
                parent,
                viewType
            )

            VIEW_TYPE_EMPTY -> {
                val emptyView = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(0, 0)
                    visibility = View.GONE
                }
                object : GridViewHolder(emptyView) {
                    override fun bind(position: Int, gridAdapter: GridAdapter) {}
                }
            }

            else -> error("Wrong view type $viewType")
        }
    }

    private fun onCreateStoryHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.fusion_async_cell, parent, false)
                .also {
                    with(it as AsyncCell) {
                        when (viewType) {
                            VIEW_TYPE_STORY, VIEW_TYPE_STORY_WEB_EMBED -> setLayoutId(R.layout.fusion_cell_story)
                            VIEW_TYPE_STORY_FULL_CARD, VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD -> setLayoutId(
                                R.layout.fusion_cell_story_full_card
                            )

                            VIEW_TYPE_STORY_TOP_CARD, VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD -> setLayoutId(
                                R.layout.fusion_cell_story_top_card
                            )

                            VIEW_TYPE_STORY_BOTTOM_CARD, VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD -> setLayoutId(
                                R.layout.fusion_cell_story_bottom_card
                            )

                            VIEW_TYPE_STORY_MIDDLE_CARD, VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD -> setLayoutId(
                                R.layout.fusion_cell_story_middle_card
                            )

                            VIEW_TYPE_STORY_FULL_CARD_FULL_BLEED, VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_FULL_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_full_card_full_bleed
                            )

                            VIEW_TYPE_STORY_TOP_CARD_FULL_BLEED, VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_FULL_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_top_card_full_bleed
                            )

                            VIEW_TYPE_STORY_BOTTOM_CARD_FULL_BLEED, VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_FULL_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_bottom_card_full_bleed
                            )

                            VIEW_TYPE_STORY_MIDDLE_CARD_FULL_BLEED, VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_FULL_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_middle_card_full_bleed
                            )

                            VIEW_TYPE_STORY_FULL_CARD_CONTAINER_BLEED, VIEW_TYPE_STORY_WEB_EMBED_FULL_CARD_CONTAINER_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_full_card
                            )

                            VIEW_TYPE_STORY_TOP_CARD_CONTAINER_BLEED, VIEW_TYPE_STORY_WEB_EMBED_TOP_CARD_CONTAINER_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_top_card
                            )

                            VIEW_TYPE_STORY_BOTTOM_CARD_CONTAINER_BLEED, VIEW_TYPE_STORY_WEB_EMBED_BOTTOM_CARD_CONTAINER_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_bottom_card
                            )

                            VIEW_TYPE_STORY_MIDDLE_CARD_CONTAINER_BLEED, VIEW_TYPE_STORY_WEB_EMBED_MIDDLE_CARD_CONTAINER_BLEED -> setLayoutId(
                                R.layout.fusion_cell_story_middle_card
                            )

                            else -> setLayoutId(R.layout.fusion_cell_story)
                        }
                        enableRiffleEffect(true)
                        inflate()
                    }
                }
        return StoryViewHolder(view, videoActivityViewModel, userHistoryViewModel)
    }

    private fun onCreateLabelHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.fusion_async_cell, parent, false)
                .also {
                    with(it as AsyncCell) {
                        when (viewType) {
                            VIEW_TYPE_LABEL_TOP_CARD -> setLayoutId(R.layout.fusion_cell_label_top_card)
                            VIEW_TYPE_LABEL_MIDDLE_CARD -> setLayoutId(R.layout.fusion_cell_label_middle_card)
                            VIEW_TYPE_LABEL_BOTTOM_CARD -> setLayoutId(R.layout.fusion_cell_label_bottom_card)
                            else -> setLayoutId(R.layout.fusion_cell_label)
                        }
                        enableRiffleEffect(true)
                        inflate()
                    }
                }
        return LabelViewHolder(view)
    }

    private fun onCreateAdHolder(parent: ViewGroup): GridViewHolder {
        val view = environment.getAdViewFactory().getAdContainer(parent)
        view.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        return AdViewHolder(view, JTidTracker.currentJTid, isScreenXSmall)
    }

    private fun onCreateSeparatorHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        return if (viewType == VIEW_TYPE_SEPARATOR_MIDDLE_CARD) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.fusion_cell_separator_middle_card, parent, false)
                .run { SeparatorHolder(this) }
        } else {
            //adding empty view, separator spacing and lines handled in Decorator classes
            SeparatorHolder(Space(parent.context))
        }
    }

    private fun onCreateVoteHolder(parent: ViewGroup): GridViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.vote, parent, false)
            .run { VoteHolder(this) }
    }

    private fun onCreateElectionsDelayHolder(parent: ViewGroup): GridViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.elections_delay, parent, false)
            .run { ElectionsDelayHolder(this) }
    }

    private fun onCreateCarouselHolder(parent: ViewGroup): GridViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.section_carousel, parent, false)
            .run {
                CarouselViewHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    parent
                )
            }
    }

    private fun onCreateStackHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_STACK_FULL_CARD -> R.layout.section_stack_full_card
            VIEW_TYPE_STACK_TOP_CARD -> R.layout.section_stack_top_card
            VIEW_TYPE_STACK_MIDDLE_CARD -> R.layout.section_stack_middle_card
            VIEW_TYPE_STACK_BOTTOM_CARD -> R.layout.section_stack_bottom_card
            else -> R.layout.section_stack
        }
        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run { StackViewHolder(this) }
    }

    /**
     * Create Global Banner Holder by inflating appropriate view xml.
     */
    private fun onCreateGlobalBannerHolder(parent: ViewGroup): GridViewHolder {
        return GlobalBannerBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
            .run {
                GlobalBannerHolder(this, environment, onImpressionEvent)
            }
    }

    private fun onCreateLowDataModalBannerHolder(parent: ViewGroup): GridViewHolder {
        return LowDataBannerHolder(ComposeView(parent.context), environment) {
            onLowDataModeDisable?.let { it() }
        }
    }

    private fun onCreateInlineOfferViewHolder(parent: ViewGroup): GridViewHolder {
        var wpGridView: WPGridView = parent as WPGridView
        val offerData = environment.getSectionInlineMessage()
        return InlineOfferHomepageHolder(
            ComposeView(parent.context),
            wpGridView.getColumnCount() == 1,
            offerData,
            shouldSuppressAds = environment.shouldSuppressAds(), onClick = {
                it?.let {
                    onMessageBannerClicked?.invoke(BannerEvent.SectionInLineClicked(it))
                }
            },
            onImpressionEvent
        )
    }

    private fun onCreateCarouselVideoHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CAROUSEL_VIDEO_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_CAROUSEL_VIDEO_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_CAROUSEL_VIDEO_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_CAROUSEL_VIDEO_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }
        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselVideoHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    parent
                )
            }
    }

    private fun onCreateCarouselAudioHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CAROUSEL_AUDIO_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_CAROUSEL_AUDIO_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_CAROUSEL_AUDIO_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_CAROUSEL_AUDIO_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselAudioHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    parent
                )
            }
    }

    private fun onCreateCarouselAudioPlaylistHolder(
        parent: ViewGroup,
        viewType: Int
    ): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_CAROUSEL_AUDIO_PLAYLIST_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselAudioPlaylistHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    parent
                )
            }
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        this.nowPlayingAudioItem = nowPlayingAudioItem
        for (i in 0..itemCount - 1) {
            val viewHolder = attachedRecyclerView?.findViewHolderForAdapterPosition(i)
            if (viewHolder is CarouselAudioPlaylistHolder) {
                viewHolder.onNowPlayingAudioItem(nowPlayingAudioItem)
            }
            if (viewHolder is CarouselAudioHolder) {
                viewHolder.onNowPlayingAudioItem(nowPlayingAudioItem)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerView = null
    }

    private fun onCreateCarouselImmersionHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_IMMERSION_CAROUSEL_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_IMMERSION_CAROUSEL_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_IMMERSION_CAROUSEL_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_IMMERSION_CAROUSEL_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselImmersionHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    environment.isNightModeEnabled(),
                    parent
                )
            }
    }

    private fun onCreateCarouselRecipeHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_RECIPE_CAROUSEL_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_RECIPE_CAROUSEL_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_RECIPE_CAROUSEL_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_RECIPE_CAROUSEL_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselRecipeHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    parent
                )
            }
    }

    private fun onCreateCarouselCommentsHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.fusion_comments, parent, false)
            .run {
                CarouselCommentsHolder(this, environment.getCarouselNetworkRequestsHelper(), parent)
                    .apply {
                        onCarouselItemClicked = { link, itemIndex ->
                            onCarouselCommentsCardClicked?.invoke(link, itemIndex)
                        }
                    }
            }
    }

    private fun onCreateCarouselExternalHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_EXTERNAL_CAROUSEL_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_EXTERNAL_CAROUSEL_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_EXTERNAL_CAROUSEL_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_EXTERNAL_CAROUSEL_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselExternalHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    environment.isNightModeEnabled(),
                    parent
                )
            }
    }

    private fun onCreateCarouselSevenLiveHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_SEVEN_LIVE_CAROUSEL_FULL_CARD -> R.layout.section_carousel_full_card
            VIEW_TYPE_SEVEN_LIVE_CAROUSEL_TOP_CARD -> R.layout.section_carousel_top_card
            VIEW_TYPE_SEVEN_LIVE_CAROUSEL_MIDDLE_CARD -> R.layout.section_carousel_middle_card
            VIEW_TYPE_SEVEN_LIVE_CAROUSEL_BOTTOM_CARD -> R.layout.section_carousel_bottom_card
            else -> R.layout.section_carousel
        }

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                CarouselSevenLiveHolder(
                    this,
                    environment.getCarouselNetworkRequestsHelper(),
                    environment.isNightModeEnabled(),
                    parent
                )
            }
    }

    private fun onCreateHabitTilesHolder(parent: ViewGroup, viewType: Int): HabitTilesHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_HABIT_TILES_FULL_CARD -> R.layout.stubbed_form_full_card
            VIEW_TYPE_HABIT_TILES_TOP_CARD -> R.layout.stubbed_form_top_card
            VIEW_TYPE_HABIT_TILES_MIDDLE_CARD -> R.layout.stubbed_form_middle_card
            VIEW_TYPE_HABIT_TILES_BOTTOM_CARD -> R.layout.stubbed_form_bottom_card
            else -> R.layout.stubbed_form
        }
        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .run {
                HabitTilesHolder(
                    view = this,
                    cardified = layoutId != R.layout.stubbed_form,
                    onHabitTileClicked = onHabitTileClicked,
                    userHistoryViewModel = userHistoryViewModel,
                    sectionsHabitTilesRequestId = sectionsHabitTilesRequestId,
                    sectionsHabitTilesTestGroup = sectionsHabitTilesTestGroup
                )
            }
    }

    private fun onCreateNewsprintTopCardHolder(parent: ViewGroup): GridViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.newsprint_top_card, parent, false)
            .run {
                NewsprintTopCardViewHolder(
                    itemView = this,
                    newsprintViewModel = newsprintViewModel,
                    onNewsprintButtonClicked = onNewsprintButtonClicked,
                    onNewsprintSpanClicked = onNewsprintSpanClicked
                )
            }
    }

    private fun onCreateSectionTopperHolder(parent: ViewGroup, viewType: Int): SectionTopperHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.fusion_section_topper, parent, false)
            .run {
                SectionTopperHolder(this, parent)
            }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        val item = items[position]
        if (item is HomepageStory) {
            val itemHashCode = getItemHashCode(item, context)
            if (itemHashCode != null) {
                return itemHashCode.toLong()
            }
        }
        return position.toLong()
    }

    /****
     *  This logic handles getting all the items id's and inserting all the unique id in the List
     *  Once we have list of Id's , Breaking the list to 4 sets using chunked
     *  Chunked will help to Split this collection into a list of lists each not exceeding the given size
     *  The last list in the resulting list may have less elements than the given size
     */
    private fun findHomePageItems() {
        val allowedItems = listOf(HomepageStory::class)
        items.forEach { item ->
            if (item::class in allowedItems && item.id is String) {
                item.id?.let {
                    idList.add(it)
                }
            }
        }
        if (idList.size > 0) {
            val chunkSize = (idList.size / 4).takeIf { it > 0 } ?: 1
            breakFeaturesId =
                idList.chunked(chunkSize) { it.toSet() } as ArrayList<Set<String>>
        }
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(position, this)
        if (holder is AdViewHolder) {
            adViews.put(position, holder.itemView)
        }
        if (holder is CarouselViewHolder) {
            carouselViews.put(position, holder.itemView)
        }
        if (holder is CarouselVideoHolder) {
            carouselVideoViews.put(position, holder.itemView)
        }
        if (holder is CarouselAudioHolder) {
            carouselAudioViews.put(position, holder.itemView)
        }
        if (holder is CarouselAudioPlaylistHolder) {
            carouselAudioPlaylistViews.put(position, holder.itemView)
        }
        if (holder is CarouselImmersionHolder) {
            carouselImmersionViews.put(position, holder.itemView)
        }
        if (holder is CarouselRecipeHolder) {
            carouselRecipeViews.put(position, holder.itemView)
        }
        if (holder is StackViewHolder) {
            stackViews.put(position, holder.itemView)
        }
        if (holder is CarouselCommentsHolder) {
            carouselCommentsViews.put(position, holder.itemView)
        }
        if (holder is CarouselExternalHolder) {
            carouselExternalViews.put(position, holder.itemView)
        }
        if (holder is CarouselSevenLiveHolder) {
            carouselSevenLiveViews.put(position, holder.itemView)
        }
        if (holder is StoryViewHolder) {
            (items[position] as? HomepageStory)?.webComponent?.let {
                storyWebEmbedViews.put(position, holder.itemView)
            }
        }
    }

    override fun onViewRecycled(holder: GridViewHolder) {
        super.onViewRecycled(holder)
        if (isCacheExtensionView(holder)) {
            // CacheExtension view should not be released here.
            // releaseViewCacheExtensions() will release the resources when needed.
        } else {
            holder.unbind()
        }
    }

    internal fun getAdapterPositionByItemId(id: Long, context: Context): Int {
        for (i in items.indices) {
            if (items[i] is HomepageStory) {
                val featureItem = items[i] as HomepageStory
                val hashCode = getItemHashCode(featureItem, context)
                if (hashCode != null && hashCode == id.toInt()) {
                    return i
                }
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun getItemHashCode(featureItem: HomepageStory?, context: Context): Int? {
        featureItem?.media?.video?.let {
            val audioMediaId = featureItem.audio?.mediaId ?: ""
            return (featureItem.media.caption
                    + featureItem.media.url
                    + featureItem.media.video.getStreamUrl()
                    + featureItem.media.video.youtubeId
                    + audioMediaId).hashCode()
        }
        return null
    }

    /**
     * Callback when GridView scroll state is idle
     */
    fun onScrollStateIdle(parent: RecyclerView) {
        var visibleVideoHolder: CarouselVideoHolder? = null
        // Make sure to autoplay the item from the first carousel that is fully visible and release all remaining carousels.
        for (i in 0 until carouselVideoViews.size()) {
            val vh =
                parent.getChildViewHolder(carouselVideoViews[carouselVideoViews.keyAt(i)]) as? CarouselVideoHolder
            if (visibleVideoHolder == null && UiUtils.isViewHeightFullyVisibleOnScreen(vh?.itemView)) {
                visibleVideoHolder = vh
                vh?.onScrollStateIdle(this)
                break
            } else {
                vh?.release()
            }
        }
        for (i in 0 until parent.childCount) {
            val childView = parent.getChildAt(i)
            val adapterPosition = parent.getChildAdapterPosition(childView)
            if (adapterPosition == -1) continue
            handleMediaOnScrollStateIdle(parent, childView, adapterPosition)
        }
        // Update Audio Carousel with scroll idle event to scroll to now playing item.
        for (i in 0 until carouselAudioViews.size()) {
            val vh =
                parent.getChildViewHolder(carouselAudioViews[carouselAudioViews.keyAt(i)]) as? CarouselAudioHolder
            vh?.onScrollStateIdle(UiUtils.isViewVisibleOnScreen(vh.itemView))
        }

        // Update Audio Playlist Carousel with scroll idle event to scroll to now playing item.
        for (i in 0 until carouselAudioPlaylistViews.size()) {
            val vh =
                parent.getChildViewHolder(
                    carouselAudioPlaylistViews[carouselAudioPlaylistViews.keyAt(
                        i
                    )]
                ) as? CarouselAudioPlaylistHolder
            vh?.onScrollStateIdle(UiUtils.isViewVisibleOnScreen(vh.itemView))
        }
    }

    /**
     *  Handle media view when scroll state is Idle
     *  Start evaluating autoplay and looping rules for visible video items.
     */
    private fun handleMediaOnScrollStateIdle(
        parent: RecyclerView?,
        itemView: View?,
        adapterPosition: Int?
    ) {
        parent ?: return
        itemView ?: return
        adapterPosition ?: return
        val activity = parent.findActivityOfType<SectionActivity>()
        // Return when activity is finishing. PostTvPlayer2Coordinator will take care of finishing case.
        if (activity?.isActivityFinishing == true) return
        val appContext = parent.context.applicationContext
        val videoManager2 = (appContext as PostTvApplication).videoManager2
        val storyView =
            ((itemView as? AsyncCell)?.getLayoutView())?.findViewById(R.id.story_view) as? HomepageStoryView2
        val story = items[adapterPosition] as? HomepageStory
        val video = story?.media?.video
        val player = videoManager2.getPlayerManager(story?.media?.getVideoId())
        if (video != null && storyView != null) {
            storyView.mediaView.let {
                // Bind a video if story view is fully visible on the screen otherwise unload it.
                if (UiUtils.isViewHeightFullyVisibleOnScreen(it)) {
                    val vh =
                        parent.findViewHolderForAdapterPosition(adapterPosition) as? StoryViewHolder
                    vh?.bindAutoplayVideo(story, story.media, video, it)
                } else {
                    //Queue event when a user scrolls past a video if it is playing or on autoplay
                    val mediaAspectRatio = story.media.aspectRatio
                    player?.let { playerManager ->
                        if (playerManager.isPlaying() && mediaAspectRatio < 1) {
                            userHistoryViewModel?.writeVideoViewedEvent(
                                id = video.omniture?.contentId,
                                section = FRONT_HOMEPAGE,
                                autoplayDuration = playerManager.getPlaybackPosition(),
                                watchDuration = null,
                                totalMilliSeconds = playerManager.getDuration(),
                                videoConclusionState = VideoConclusionState.SCROLLED_THROUGH
                            )
                        }
                    }
                    videoManager2.onVideoOffscreen(story.media.getVideoId())
                }
            }
        }
    }

    /**
     * Callback when CarouselView scroll state is idle
     */
    fun onVideoCarouselStateIdle(videoHolder: CarouselVideoHolder) {
        // Check if videoHolder height is fully visible.
        if (UiUtils.isViewHeightFullyVisibleOnScreen(videoHolder.itemView)) {
            videoHolder.onScrollStateIdle(this)
        }
    }

    /**
     * Callback when any video item starts playing in a CarouselVideoHolder
     */
    fun onCarouselVideoStartsPlaying(videoHolder: CarouselVideoHolder) {
        val parent = videoHolder.itemView.parent as? RecyclerView
        // release all remaining CarouselVideoHolders when given videoHolder starts playing.
        for (i in 0 until carouselVideoViews.size()) {
            val vh =
                parent?.getChildViewHolder(carouselVideoViews[carouselVideoViews.keyAt(i)]) as? CarouselVideoHolder
            if (videoHolder != vh) {
                vh?.release()
            }
        }
    }

    fun onPageSelected(parent: RecyclerView) {
        for (i in 0 until carouselVideoViews.size()) {
            val vh =
                parent.getChildViewHolder(carouselVideoViews[carouselVideoViews.keyAt(i)]) as? CarouselVideoHolder
            vh?.release()
        }
        refreshAds(parent)
    }

    fun clearData() {
        items.clear()
        idList.clear()
        breakFeaturesId.clear()
        notifyDataSetChanged()
    }

    private fun isCacheExtensionView(holder: GridViewHolder): Boolean {
        return when (holder) {
            is AdViewHolder,
            is CarouselViewHolder,
            is CarouselVideoHolder,
            is CarouselAudioHolder,
            is CarouselAudioPlaylistHolder,
            is CarouselImmersionHolder,
            is CarouselRecipeHolder,
            is StackViewHolder,
            is CarouselCommentsHolder,
            is CarouselExternalHolder,
            is CarouselSevenLiveHolder
                -> true

            is StoryViewHolder ->
                return storyWebEmbedViews.containsValue(holder.itemView)

            else -> false
        }
    }
}

abstract class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(position: Int, gridAdapter: GridAdapter)
    open fun unbind() = Unit
}

class StoryViewHolder(
    itemView: View,
    private val videoActivityViewModel: SectionVideoActivityViewModel?,
    private val userHistoryViewModel: UserHistoryViewModel?
) : GridViewHolder(itemView) {

    val classTag: String = StoryViewHolder::class.java.simpleName
    lateinit var environment: GridEnvironment
    private var isCardified: Boolean = false

    private val sectionActivity get() = itemView.findActivityOfType<SectionActivity>()


    override fun bind(position: Int, gridAdapter: GridAdapter) {
        environment = gridAdapter.environment
        isCardified = listOf(
            VIEW_TYPE_STORY_FULL_CARD,
            VIEW_TYPE_STORY_TOP_CARD,
            VIEW_TYPE_STORY_MIDDLE_CARD,
            VIEW_TYPE_STORY_BOTTOM_CARD
        ).contains(itemViewType)
        (itemView as AsyncCell).bindWhenInflated {
            // Return when items[position] do not match with the HomepageStory.
            // Items could have been updated in the bg when inflation is doing its work for the previous grid.
            if (position >= gridAdapter.items.size || gridAdapter.items[position] !is HomepageStory) {
                val e =
                    IllegalStateException("Item is not matching! pos=$position, section=${gridAdapter.grid?.tracking?.pageName}")
                environment.remoteLogError(classTag, e)
                return@bindWhenInflated
            }
            val homepageStoryView =
                getLayoutView()!!.findViewById<HomepageStoryView2>(R.id.story_view)
            with(homepageStoryView) {
                setNightMode(gridAdapter.environment.isNightModeEnabled())
                setLiveBlogProxyUrl(gridAdapter.environment.getLiveBlogProxyUrl())
                setIsCardified(isCardified)
                val storyItem = gridAdapter.items[position] as HomepageStory
                checkPercentageForFusionSection(storyItem.id, position, gridAdapter)
                setFeatureItem(
                    storyItem,
                    gridAdapter.animatedImageLoader,
                    itemId
                )
                setOnClickListener {
                    val story = (gridAdapter.items[position] as HomepageStory)
                    gridAdapter.onStoryViewClicked?.invoke(story, story.adapterPosition)
                }
                gridAdapter.onActionButtonClicked?.let {
                    initActionClick(it)
                }
                gridAdapter.onStoryViewBlurbsLinkClicked?.let { onClick ->
                    initBlurbsLinkClick { onClick.invoke(storyItem, it) }
                }

                mediaView?.setOnClickListener { cellMediaView ->
                    val story = (gridAdapter.items[position] as HomepageStory)
                    val video = story.media?.video
                    if (video != null) {
                        if (shouldPlayAds(video) && environment.isAdsContentContextualTargetingEnabled() && !(story.media.aspectRatio < 1)) {
                            observeAdsContentUiStateEvents(story, mediaView)
                            videoActivityViewModel?.dispatchVideoClickEvent(
                                video.omniture?.contentId
                            )
                        } else {
                            bindUserInitiatedVideo(
                                story,
                                story.media,
                                video,
                                cellMediaView,
                                null,
                                userHistoryViewModel
                            )
                        }
                    } else if (story.media?.link != null) {
                        gridAdapter.onMediaClicked?.invoke(story, story.media.link)
                    }
                }
                val isMediaClickable =
                    (storyItem.media?.video != null && !storyItem.media.video.isLooping) || storyItem.media?.link != null
                mediaView?.setIsClickable(isMediaClickable)
                mediaView?.bringToFront()
                if (isMediaClickable && storyItem.media?.mediaType != MediaType.YOUTUBE
                    && storyItem.media?.video?.isLive == false
                    && sectionActivity?.canAutoPlayInlineVideo() == true
                ) {
                    // Cache video url if it is in a visible range
                    storyItem.media.video.getStreamUrl()?.let {
                        if (it.isNotEmpty())
                            ExoPlayerCache.cacheVideo(it)
                    }
                    storyItem.media.video.promo?.url?.let {
                        if (it.isNotEmpty() && !storyItem.media.video.autoplay)
                            ExoPlayerCache.cacheVideo(it)
                    }
                }
                relatedLinksView?.callBack = object : RelatedLinksView.RelatedLinksCallback {
                    override fun onRelatedLinkClicked(relatedLinkItem: RelatedLinkItem) {
                        val story = (gridAdapter.items[position] as HomepageStory)
                        gridAdapter.onRelatedLinkClicked?.invoke(story, relatedLinkItem)
                    }
                }
                liveImageView?.liveImageCallback =
                    object : LiveImageContainerView.LiveImageCallback {
                        override fun onLiveImageClicked() {
                            if (storyItem.media?.link != null) {
                                gridAdapter.onMediaClicked?.invoke(storyItem, storyItem.media.link)
                            } else {
                                gridAdapter.onStoryViewClicked?.invoke(
                                    storyItem,
                                    storyItem.adapterPosition
                                )
                            }
                        }

                    }

                slideShowContainerView?.slideShowOverlayCallback =
                    object : SlideShowContainerView.SlideShowOverlayCallback {
                        override fun onSlideShowOverlayClicked() {
                            if (storyItem.media?.link != null) {
                                gridAdapter.onMediaClicked?.invoke(storyItem, storyItem.media.link)
                            } else {
                                gridAdapter.onStoryViewClicked?.invoke(
                                    storyItem,
                                    storyItem.adapterPosition
                                )
                            }
                        }
                    }

                liveBlogView?.setBlogItemClickListener {
                    gridAdapter.onLiveBlogClicked?.invoke(it, storyItem.itId)
                }
                compoundLabelView?.let { compoundLabelView ->
                    bindClickListener(storyItem.label?.link, compoundLabelView) { link, view ->
                        storyItem.label?.let { environment.openLabel(it, storyItem.itId) }
                    }
                }
                ctaView?.let { ctaView ->
                    bindClickListener(storyItem.cta?.link, ctaView) { link, view ->
                        storyItem.cta?.let { environment.openLabel(it, storyItem.itId) }
                    }
                }
                topperLabelView?.let { topperLabelView ->
                    bindClickListener(storyItem.topperLabel?.link, topperLabelView) { link, view ->
                        storyItem.topperLabel?.let { environment.openLabel(it, storyItem.itId) }
                    }
                }
                footNoteView?.let { footNoteView ->
                    bindClickListener(storyItem.footNote?.link, footNoteView) { link, view ->
                        storyItem.footNote?.link?.url?.let { environment.openLink(it) }
                    }
                }
                olympicsMedalsView?.setCtaClickListener {
                    gridAdapter.environment.openLink(it)
                }
                if (storyItem.isNewsprint) {
                    if (gridAdapter.isScreenXSmall) { // apply gradient to cardified version only
                        NewsprintHelper.applyNewsprintGradient(this, storyItem)
                    }
                    this.applyNewsprintTextColor()
                    val shouldShowNewsprintBadge =
                        gridAdapter.newsprintViewModel?.shouldShowNewsprintBadge(storyItem) ?: false
                    val badgeView =
                        getLayoutView()!!.findViewById<ComposeView?>(R.id.newsprint_this_is_you_view)
                    if (shouldShowNewsprintBadge) {
                        badgeView?.setContent {
                            NewsprintBadgeOverlay()
                        }
                        badgeView?.visibility = View.VISIBLE
                    } else {
                        badgeView?.visibility = View.GONE
                    }
                } else {
                    this.applyStoryTextColor()
                }
            }
        }
    }

    fun bindAutoplayVideo(
        story: HomepageStory,
        media: Media,
        video: com.wapo.flagship.features.grid.model.Video,
        cellMediaView: View
    ) {
        // Skip Autoplay when
        // 1. user has already initiated that video
        // 2. autoplay is off in the app settings
        // 3. autoplay value is false and isLooping value is false and there is no promo url in the feed
        // 4. mediaType is youtube
        val appContext = itemView.context.applicationContext
        val videoManager2 = (appContext as PostTvApplication).videoManager2
        if (videoManager2.getPlayerFrame(media.getVideoId())?.video?.playType == Video.PLAY_TYPE_NORMAL) return
        if (sectionActivity?.canAutoPlayInlineVideo() == false) return
        if (!video.autoplay && !video.isLooping && video.promo?.url.isNullOrEmpty()) return
        if (media.mediaType == MediaType.YOUTUBE) return
        val playType =
            if (video.autoplay && video.isLooping) Video.PLAY_TYPE_NORMAL_MUTED else Video.PLAY_TYPE_AUTOPLAY
        bindVideo(story, media, video, cellMediaView, playType, null)
        (cellMediaView as CellMediaView).let {
            if (playType == Video.PLAY_TYPE_AUTOPLAY) it.bringOverlayToFront()
        }
    }

    fun bindUserInitiatedVideo(
        story: HomepageStory,
        media: Media,
        video: com.wapo.flagship.features.grid.model.Video,
        cellMediaView: View,
        sectionAdsTargetingContent: TargetingContent?,
        userHistoryViewModel: UserHistoryViewModel?
    ) {
        // Skip allowing [Video.PLAY_TYPE_NORMAL_MUTED] videos when autoplay is enabled in the app settings.
        // They will autoplay and loop based on their view holders visibility.
        val playType =
            if (video.autoplay && video.isLooping) Video.PLAY_TYPE_NORMAL_MUTED else Video.PLAY_TYPE_NORMAL
        if (playType == Video.PLAY_TYPE_NORMAL_MUTED && sectionActivity?.canAutoPlayInlineVideo() == true) return
        bindVideo(story, media, video, cellMediaView, playType, sectionAdsTargetingContent)
    }

    private fun bindVideo(
        story: HomepageStory,
        media: Media,
        video: com.wapo.flagship.features.grid.model.Video,
        cellMediaView: View,
        playType: Int,
        sectionAdsTargetingContent: TargetingContent?
    ) {
        val mediaView = cellMediaView as? CellMediaView ?: return
        val appContext = itemView.context.applicationContext
        val videoManager2 = (appContext as PostTvApplication).videoManager2
        val isYouTube = media.mediaType == MediaType.YOUTUBE
        val videoId: String? = media.getVideoId()
        if (videoId == null) {
            val toast =
                Toast.makeText(itemView.context, R.string.video_not_available, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            return
        }
        val isLive: Boolean = video.isLive
        val pageName: String? = video.omniture?.pageName
        val videoName: String? = video.omniture?.videoName
        val videoSection: String? = video.omniture?.videoSection
        val videoSource: String? = video.omniture?.videoSource
        val videoCategory: String? = video.omniture?.videoCategory
        val contentId: String? = video.omniture?.contentId
        val shareUrl = story.link?.url
        val headline = story.headline?.text ?: "Video"
        val isLooping = video.isLooping
        val autoplay = video.autoplay
        val isPromoLooping = video.promo?.isLooping
        val promoUrl = video.promo?.url
        val adTagUrl = if (environment.shouldSuppressAds() || video.adConfig?.playVideoAds == false) null else getAdTagUrl(
            video,
            story,
            sectionAdsTargetingContent
        )
        val model = Video.Builder()
            .setId(videoId)
            .setIsYouTube(isYouTube)
            .setIsLive(isLive)
            .setShareUrl(shareUrl)
            .setHeadline(headline)
            .setPageName(pageName)
            .setVideoName(videoName)
            .setVideoSection(videoSection)
            .setVideoSource(videoSource)
            .setDuration(video.duration ?: 0L)
            .setVideoCategory(videoCategory)
            .setContentId(contentId)
            .setAdTagUrl(adTagUrl)
            .setShouldPlayAds(shouldPlayAds(video))
            .setIsLooping(isLooping)
            .setAutoplay(autoplay)
            .setPromoIsLooping(isPromoLooping)
            .setPromoUrl(promoUrl)
            .setPlayType(playType)
            .setAspectRatio(media.aspectRatio)
            .setPlaybackPosition(
                if (playType == Video.PLAY_TYPE_NORMAL)
                    mediaView.getPlaybackPosition(videoManager2, videoId)
                else -1
            )
            .build()

        videoManager2.initMedia(
            model,
            isPlayerClickable = media.aspectRatio < 1 && media.video?.isLooping == false
        ) {
            val player = videoManager2.getPlayerManager(videoId)
            userHistoryViewModel?.writeVideoViewedEvent(
                id = contentId,
                section = FRONT_HOMEPAGE,
                autoplayDuration = if (autoplay) player?.getPlaybackPosition() else null,
                watchDuration = if (!autoplay) player?.getPlaybackPosition() else null,
                totalMilliSeconds = player?.getDuration()?.takeIf { it > 0 }
                    ?: model.duration.takeIf { it > 0 }?.times(1000L),
                videoConclusionState = VideoConclusionState.VIDEO_OPENED
            )
            if (media.aspectRatio < 1) {
                val videos = listOf(model)
                environment.openWatchVideoCard(videos, videoSection ?: "", "top stories", 0, 0)
            }
        }

        videoManager2.getPlayerManager(videoId)
            ?.showControllerOptions(
                com.wapo.flagship.features.posttv.R.id.exo_share,
                com.wapo.flagship.features.posttv.R.id.exo_fullscreen,
                com.wapo.flagship.features.posttv.R.id.exo_pip
            )
        if (playType == Video.PLAY_TYPE_AUTOPLAY || playType == Video.PLAY_TYPE_NORMAL_MUTED) {
            mediaView.displayVideoOnceReady(videoManager2, videoId, itemId)
        } else {
            mediaView.displayVideo(videoManager2, videoId, itemId)
        }
    }

    private fun shouldPlayAds(video: com.wapo.flagship.features.grid.model.Video): Boolean {
        val adConfig = video.adConfig ?: return false
        return adConfig.playVideoAds && !environment.shouldSuppressAds()
    }

    private fun getAdTagUrl(
        video: com.wapo.flagship.features.grid.model.Video,
        story: HomepageStory,
        targetingContent: TargetingContent?
    ): String? {
        return environment.getAdTagUrl(video, story, targetingContent)
    }

    private fun observeAdsContentUiStateEvents(story: HomepageStory, mediaView: CellMediaView) {
        val video = story.media?.video
        val videoId = video?.omniture?.contentId
        if (video != null && !videoId.isNullOrEmpty()) {
            val lifecycleOwner =
                itemView.findViewTreeLifecycleOwner() ?: itemView.context.findComponentActivity()
                ?: return
            videoActivityViewModel?.createContentUIState(videoId)
            val uiState = videoActivityViewModel?.targetingContentUIStateMap?.get(videoId)
            uiState?.removeObservers(lifecycleOwner)
            uiState?.observe(lifecycleOwner) { state ->
                state ?: return@observe
                if (videoId != state.id) return@observe
                when (state) {
                    is TargetingContentUIState.Loading -> {
                        mediaView.showProgressBar()
                    }

                    is TargetingContentUIState.Cancelled -> {
                        uiState.removeObservers(lifecycleOwner)
                        videoActivityViewModel?.clearContentUIState(videoId)
                        mediaView.hideProgressBar()
                    }

                    is TargetingContentUIState.Content, is TargetingContentUIState.Error, is TargetingContentUIState.UITimeout -> {
                        val adsContentItem =
                            if (state is TargetingContentUIState.Content) state.items.firstOrNull { it.id == videoId } else null
                        bindUserInitiatedVideo(
                            story,
                            story.media,
                            video,
                            mediaView,
                            adsContentItem,
                            userHistoryViewModel
                        )
                        uiState.removeObservers(lifecycleOwner)
                        videoActivityViewModel?.clearContentUIState(videoId)
                        mediaView.hideProgressBar()
                    }
                }
            }
        }
    }

    private fun <T> bindClickListener(obj: T?, view: View, action: (T, View) -> Unit) {
        if (obj != null) {
            view.setOnClickListener {
                action(obj, view)
            }
        } else {
            view.setOnClickListener(null)
        }
    }

    /****
     *  This function is to calculate the percentage scrolled based on the total feature ids w.r.t to its position
     *  Compares the id of the visible item with the Index of feature id's in the featureBreakId's sets
     *  returns the index value based on the feature id,s in the featureBreakIds sets
     */

    private fun checkPercentageForFusionSection(
        featureID: String?,
        position: Int,
        adapter: GridAdapter
    ) {
        val totalFeatureItems = adapter.idList.size
        val featureBreakIDs = adapter.breakFeaturesId
        val grid = adapter.grid
        var count = 0
        featureBreakIDs.forEachIndexed { index, list ->
            if (list.contains(featureID)) {
                count = if (list.last() == featureID) {
                    index + 1
                } else
                    index
            }

            updatePercentageEvents(adapter, count, totalFeatureItems)
        }
    }

    /****
     *  Based on the above index value retuned this function helps to trigger percentage calculated based ont he scroll
     *  Maintaining a event set to make sure the events are not triggered twice,  basically avoids making duplicate events fire
     */

    private fun updatePercentageEvents(adapter: GridAdapter, count: Int, totalFeatureItems: Int) {
        val percentage = when (count) {
            1 -> 25
            2 -> 50
            3 -> 75
            4 -> 100
            else -> 0
        }
        if (percentage > 0) {
            adapter.onSectionThresholdScrolled?.invoke(percentage, totalFeatureItems)
        }
    }

    private fun isCardified(): Boolean {
        return listOf(
            VIEW_TYPE_STORY_FULL_CARD,
            VIEW_TYPE_STORY_TOP_CARD,
            VIEW_TYPE_STORY_MIDDLE_CARD,
            VIEW_TYPE_STORY_BOTTOM_CARD,
            VIEW_TYPE_STORY_FULL_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_TOP_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_MIDDLE_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_BOTTOM_CARD_FULL_BLEED,
            VIEW_TYPE_STORY_FULL_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_TOP_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_MIDDLE_CARD_CONTAINER_BLEED,
            VIEW_TYPE_STORY_BOTTOM_CARD_CONTAINER_BLEED
        ).contains(itemViewType)
    }
}

class LabelViewHolder(itemView: View) : GridViewHolder(itemView) {
    val classTag: String = LabelViewHolder::class.java.simpleName
    lateinit var environment: GridEnvironment
    override fun bind(position: Int, gridAdapter: GridAdapter) {
        environment = gridAdapter.environment
        (itemView as AsyncCell).bindWhenInflated {
            // Return when items[position] do not match with the LabelItem.
            // Items could have been updated in the bg when inflation is doing its work for the previous grid.
            if (position >= gridAdapter.items.size || gridAdapter.items[position] !is LabelItem) {
                val e =
                    IllegalStateException("Item is not matching! pos=$position, section=${gridAdapter.grid?.tracking?.pageName}")
                environment.remoteLogError(classTag, e)
                return@bindWhenInflated
            }
            val compoundLabelView =
                getLayoutView()!!.findViewById<CompoundLabelView>(R.id.compound_label)
            with(compoundLabelView as CompoundLabelView) {
                setCardify(itemViewType == VIEW_TYPE_LABEL_TOP_CARD || itemViewType == VIEW_TYPE_LABEL_MIDDLE_CARD)
                val labelItem = gridAdapter.items[position] as LabelItem
                if (labelItem.forceLeftAlignOnExtraSmall && environment.isPhone()) {
                    labelItem.compoundLabel.alignment = Alignment.LEFT
                }
                setLabel(labelItem.compoundLabel)
                itemView.setOnClickListener {
                    val label = (gridAdapter.items[position] as LabelItem).compoundLabel
                    gridAdapter.onLabelClicked?.invoke(label, null)
                }
            }
        }
    }
}

class AdViewHolder(itemView: View, val jTid: Long?, private val isScreenXSmall: Boolean) :
    GridViewHolder(itemView) {
    private val adView: BannerAdView? = itemView.findViewById(com.wapo.adsinf.R.id.ad_view)
    private val adLabel: TextView? = itemView.findViewById(com.wapo.adsinf.R.id.ad_label)

    init {
        itemView.setBackgroundColor(
            ContextCompat.getColor(itemView.context, R.color.section_front_background)
        )
        adLabel?.textSize = 12f
    }

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        if (gridAdapter.environment.shouldSuppressAds()) {
            AdsUtil.setAdLayoutVisibility(adLayout = itemView, isVisible = false)
            return
        }

        val adItem = gridAdapter.items[position] as Ad
        val isCardify = gridAdapter.grid?.cards?.extraSmall != null && isScreenXSmall
        val config = AdConfig(
            adUnitId = AdsUtil.getAdUnitId(
                context = itemView.context,
                contentType = adItem.contentType.orEmpty(),
                adType = adItem.adType.orEmpty(),
                adKey = formatCommercialNode(adItem.commercialNode),
            ),
            adRequestTargets = AdRequestTargets.getDefault().apply {
                val contentUrl = getAdContentUrl(adItem.commercialNode.orEmpty())
                setContentUrl(contentUrl)
                addAnalyticsTags(jTid = jTid, logEventExtras = { it.setContentUrl(contentUrl) })
                addSectionAdTargetingValues(
                    adItem.primarySectionId.orEmpty(),
                    contentUrl = contentUrl
                )
                addSlotSizeParameters(AdSlotType.SHORT)
                addBrandSuitabilityKey("garm_low")
                addAdPosition(adItem.adType.orEmpty())
            },
            adDimensions = mutableListOf<AdDimension>().apply {
                add(AdDimension.Medium)
                add(AdDimension.Fluid)
                addAll(AdsUtil.findAdDimensionsFromDeviceWidth())
            },
            networkExtras = AdsUtil.getDefaultNetworkExtras(),
            containerMinHeight = with(itemView.context.resources) {
                when {
                    isCardify -> getInteger(R.integer.sections_cardified_ad_container_min_height)
                    AppContextUtils.isTablet() -> AdsUtil.MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER_TABLET
                    else -> getInteger(R.integer.sections_ad_container_min_height)
                }
            },
            section = "front"
        )
        adView?.loadAd(config)
    }

    private fun formatCommercialNode(commercialNode: String?): String {
        val nodeBuilder = StringBuilder()

        // process commercialNode
        if (TextUtils.isEmpty(commercialNode)) nodeBuilder.append("default")
        else if (commercialNode?.startsWith("/") == true) nodeBuilder.append(
            commercialNode.substring(
                1
            )
        )
        else if (commercialNode == "washingtonpost.com") nodeBuilder.append("homepage")
        else nodeBuilder.append(commercialNode)

        // append /front
        if (nodeBuilder.toString().endsWith("/")) nodeBuilder.append("front")
        else nodeBuilder.append("/front")

        return nodeBuilder.toString()
    }

    private fun getAdContentUrl(commercialNode: String): String {
        val domain = "https://www.washingtonpost.com"

        if (TextUtils.isEmpty(commercialNode)) {
            RemoteLog.w(
                itemView.context,
                EventLog.Builder()
                    .setMessage("CommercialNode is missing in sections ads")
                    .setModule(LogModules.ADS)
                    .set("commercial_node", commercialNode)
                    .build()
            )
            return domain
        }

        //washingtonpost.com is the homepage default, just return domain
        if (commercialNode == "washingtonpost.com" || commercialNode == "www.washingtonpost.com") {
            return domain
        }


        //otherwise expecting a path like politics/transfer-of-power or /science/animals


        //add space between domain and text string if needed
        if (commercialNode.startsWith("/")) {
            return domain + commercialNode
        } else {
            return "$domain/$commercialNode"
        }
    }

    override fun unbind() {
        super.unbind()
        adView?.release()
    }
}

/**
 * View Holder for Global CTA Banner
 */
class GlobalBannerHolder(
    val binding: GlobalBannerBinding,
    val environment: GridEnvironment,
    val onImpressionEvent: ((BannerLifecycleEvent) -> Unit)?
) : GridViewHolder(binding.root) {
    // Helper that applies appropriate changes to [GlobalBannerBinding] in order to show correct Banner.
    private val viewStateHelper =
        GlobalBannerViewStateHelper(binding, binding.root.context, environment.shouldSuppressAds(), onImpressionEvent)
    private var globalBannerModel: GlobalBanner? = null
    override fun bind(position: Int, gridAdapter: GridAdapter) {
        // Get the appropriate model that holds the primary, secondary, and button text
        globalBannerModel = gridAdapter.items[position] as? GlobalBanner
        globalBannerModel?.let {
            when (it.globalBannerState) {
                // Show Subscribe Banner for when user has no Sub
                is GlobalBannerState.NoSub -> viewStateHelper.showNoSub(
                    it.globalBannerState,
                    gridAdapter.onMessageBannerClicked
                )

                // Show promotional messages (non offer related) to Subscribers.
                is GlobalBannerState.Sub -> viewStateHelper.showSub(
                    it.globalBannerState,
                    gridAdapter.onMessageBannerClicked,
                    it.globalBannerState.offer?.passedIterableGuardrails
                )
                // Hide Subscribe Banner for Sub, ConfigHideBanner, and Unknown states
                else -> viewStateHelper.hideBanner()
            }
        }
        val offer = (globalBannerModel?.globalBannerState as? GlobalBannerState.NoSub)?.offer
            ?: (globalBannerModel?.globalBannerState as? GlobalBannerState.Sub)?.offer
        offer?.attributionInfo?.let {
            onImpressionEvent?.invoke(BannerLifecycleEvent.StartImpression(it))
        }
    }

    override fun unbind() {
        val offer = (globalBannerModel?.globalBannerState as? GlobalBannerState.NoSub)?.offer
            ?: (globalBannerModel?.globalBannerState as? GlobalBannerState.Sub)?.offer
        offer?.attributionInfo?.let {
            onImpressionEvent?.invoke(BannerLifecycleEvent.EndImpression(it))
        }
    }
}

/**
 * View Holder for Low Data Banner
 */
class LowDataBannerHolder(
    val composeView: ComposeView,
    private val environment: GridEnvironment,
    private val onChangeClick: () -> Unit
) : GridViewHolder(composeView) {
    override fun bind(position: Int, gridAdapter: GridAdapter) {
        (gridAdapter.items[position] as? LowDataBanner)?.let { modal ->
            // If banner is disable check the environment value to avoid unknown state
            // TODO check multiple additions for one view in the adapter this is why we use position == 1 hardcoded
            bannerIsVisible((modal.isLowDataBannerEnable || environment.isLowDataModeEnable()) && position == 1)
        }
    }

    private fun bannerIsVisible(isVisible: Boolean) {
        composeView.apply {
            setContent {
                LowDataBannerView(
                    Modifier.padding(
                        top = 0.dp,
                        end = 0.dp,
                        start = 0.dp,
                        bottom = 7.dp
                    ),
                    isVisible,
                    onChangeClick
                )
            }
        }
    }
}

class SeparatorHolder(itemView: View) : GridViewHolder(itemView) {
    override fun bind(position: Int, gridAdapter: GridAdapter) {
        if (itemViewType == VIEW_TYPE_SEPARATOR_MIDDLE_CARD) {
            val separator = gridAdapter.items[position] as Separator
            val height = when (separator.size) {
                SeparatorSize.XSMALL -> itemView.context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_xsmall)
                SeparatorSize.SMALL -> itemView.context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_small)
                SeparatorSize.LARGE -> itemView.context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_large)
            }
            itemView.findViewById<Space>(R.id.separator_space).layoutParams =
                ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, height)
        }
        //separator spacing and lines handled in Decorator classes
    }
}


package com.wapo.flagship.features.grid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.Adapter
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.grid.model.*
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.views.vote.VoteGuideService
import com.wapo.flagship.features.pagebuilder.AdViewFactory
import com.wapo.flagship.features.sections.SectionsPagerView
import com.wapo.flagship.features.sections.SubscribeButton
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.view.habittiles.Tile
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.sections.R
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs


class WPGridView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val spaceDecorator: SpaceDecorator
    private val borderDecorator: BorderDecorator
    private var drawGrid = true
    private val columnWidth: Int
    private val gutterWidth: Int
    private val cardGutterWidth: Int
    private val chainGutterHeight: Int
    private val singleColumnMargin: Int
    private val chainLabelGutterHeight: Int
    private val separatorLargeHeight: Int
    private val separatorSmallHeight: Int
    private val separatorExtraSmallHeight: Int
    private val cardDividerPadding: Int
    private var savedState: SavedState? = null

    private var scrollListener: OnScrollListener? = null

    private var touchdownPoint: PointF = PointF(0f, 0f)
    private val touchSlop = ViewConfiguration.get(context).scaledPagingTouchSlop

    private val gridDebugPaintColumn: Paint = Paint().apply {
        color = Color.rgb(230, 226, 230)
        alpha = 50
    }

    init {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.WPGridView,
                0, 0)
        columnWidth = a.getDimensionPixelSize(R.styleable.WPGridView_column_width, context.resources.getDimensionPixelSize(R.dimen.grid_column_width))
        gutterWidth = a.getDimensionPixelSize(R.styleable.WPGridView_gutter_width, context.resources.getDimensionPixelSize(R.dimen.grid_gutter_width))
        cardGutterWidth = a.getDimensionPixelSize(R.styleable.WPGridView_card_gutter_width, context.resources.getDimensionPixelSize(R.dimen.grid_card_gutter_width))
        chainGutterHeight = a.getDimensionPixelSize(R.styleable.WPGridView_chain_gutter_height, context.resources.getDimensionPixelSize(R.dimen.grid_chain_gutter_height))
        singleColumnMargin = a.getDimensionPixelSize(R.styleable.WPGridView_single_column_margin, context.resources.getDimensionPixelSize(R.dimen.grid_single_column_margin))
        chainLabelGutterHeight = a.getDimensionPixelSize(R.styleable.WPGridView_chain_label_gutter_height, context.resources.getDimensionPixelSize(R.dimen.grid_chain_label_gutter_height))
        separatorLargeHeight = a.getDimensionPixelSize(R.styleable.WPGridView_chain_separator_large, context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_large))
        separatorSmallHeight = a.getDimensionPixelSize(R.styleable.WPGridView_chain_separator_small, context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_small))
        separatorExtraSmallHeight = a.getDimensionPixelSize(R.styleable.WPGridView_chain_separator_xsmall, context.resources.getDimensionPixelSize(R.dimen.grid_chain_separator_xsmall))
        cardDividerPadding = context.resources.getDimensionPixelSize(R.dimen.card_divider_padding)

        a.recycle()

        layoutManager = SpannableGridLayoutManager(this)

        spaceDecorator = SpaceDecorator(gutterWidth, cardGutterWidth, chainLabelGutterHeight, separatorLargeHeight, separatorSmallHeight, separatorExtraSmallHeight, chainGutterHeight)
        addItemDecoration(spaceDecorator)

        borderDecorator = BorderDecorator()
        borderDecorator.separatorPaint = Paint().apply {
            this.color = ContextCompat.getColor(context, R.color.grid_separator_color)
            this.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.grid_separator_width).toFloat()
        }
        borderDecorator.normalDividerPaint = Paint().apply {
            this.color = ContextCompat.getColor(context, R.color.grid_normal_divider_color)
            this.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.grid_separator_width).toFloat()
        }
        borderDecorator.boldDividerPaint = Paint().apply {
            this.color = ContextCompat.getColor(context, R.color.grid_bold_divider_color)
            this.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.grid_separator_width).toFloat()
        }
        addItemDecoration(borderDecorator)

        adapter = GridAdapter(context).apply {
            prepareViewCacheExtensions(this@WPGridView)
        }
        isSaveEnabled = true
        itemAnimator = DefaultItemAnimator()

        scrollListener = object: OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_SETTLING) {
                    adapter?.onScrollStateIdle(recyclerView)
                }
            }
        }

        scrollListener?.let {
            addOnScrollListener(it)
        }

        itemAnimator = null
    }

    override fun getLayoutManager(): SpannableGridLayoutManager? {
        return super.getLayoutManager() as SpannableGridLayoutManager
    }

    fun setGrid(grid: Grid?) {
        val prevGridScrollPosition = layoutManager?.getScrollPosition() ?: -1
        spaceDecorator.grid = grid
        borderDecorator.setGrid(grid)
        layoutManager?.setGrid(grid)
        adapter?.setGrid(grid)
        val savedState = savedState
        if (savedState != null && grid != null) {
            if (savedState.checksum == grid.checksum) {
                layoutManager?.scrollToPosition(savedState.scrollPosition)
            }
            this.savedState = null
            return
        } else {
            if (prevGridScrollPosition > 0) {
                layoutManager?.scrollToPosition(prevGridScrollPosition)
            }
        }
    }

    fun setDisplayContext(displayContext: String) {
        layoutManager?.setDisplayContext(displayContext)
    }

    fun setSectionDisplayName(sectionDisplayName: String) {
        layoutManager?.setSectionDisplayName(sectionDisplayName)
    }

    fun refreshAds() {
        if (adapter?.environment?.shouldSuppressAds() == true) {
            layoutManager?.getGrid()?.let { grid ->
                val scrollPosition = layoutManager?.getScrollPosition() ?: -1
                setGrid(grid)
                if (scrollPosition > 0) {
                    layoutManager?.scrollToPosition(scrollPosition)
                }
            }
        } else {
            adapter?.refreshAds(this)
        }
    }

    fun refreshHabitTiles() {
        adapter?.refreshHabitTiles(this)
    }

    fun setEnvironment(gridEnvironment: GridEnvironment) {
        (adapter as GridAdapter).environment = gridEnvironment
    }

    fun setImageLoader(animatedImageLoader: AnimatedImageLoader) {
        (adapter as GridAdapter).setImageLoader(animatedImageLoader)
    }

    fun setNightModeEnabled(isNightModeEnabled: Boolean) {
        val separatorColorResId = if (isNightModeEnabled) R.color.grid_separator_color_night else R.color.grid_separator_color
        val normalDividerColorResId = if (isNightModeEnabled) R.color.grid_normal_divider_color_night else R.color.grid_normal_divider_color
        val boldDividerColorRedId = if (isNightModeEnabled) R.color.grid_bold_divider_color_night else R.color.grid_bold_divider_color
        borderDecorator.apply {
            separatorPaint.color = ContextCompat.getColor(context, separatorColorResId)
            normalDividerPaint.color = ContextCompat.getColor(context, normalDividerColorResId)
            boldDividerPaint.color = ContextCompat.getColor(context, boldDividerColorRedId)
        }
        invalidate()
    }

    override fun getAdapter(): GridAdapter? {
        return super.getAdapter() as? GridAdapter
    }

    override fun dispatchDraw(c: Canvas) {
        super.dispatchDraw(c)
        if (!drawGrid) return
        val columnCount = getColumnCount()
        if (columnCount == 1) {
            c.drawRect(singleColumnMargin.toFloat(), 0f, (width - singleColumnMargin).toFloat(), height.toFloat(), gridDebugPaintColumn)
        } else {
            val contentWidth = columnCount * columnWidth + (columnCount - 1) * gutterWidth // N columns and N-1 gutters in between
            val sideMargin = (width - contentWidth) / 2
            for (i in 0 until columnCount) {
                val columnStart = i * (columnWidth + gutterWidth).toFloat() + sideMargin
                val columnEnd = columnStart + columnWidth.toFloat()
                c.drawRect(columnStart, 0f, columnEnd, height.toFloat(), gridDebugPaintColumn)
            }
        }
    }

    fun getColumnCount(): Int {
        return BreakPoints.getColumnCount(width.toDp(resources.displayMetrics.density))
    }

    fun getColumnWidth(): Int {
        return if (getColumnCount() == 1) {
            width - singleColumnMargin * 2
        } else {
            columnWidth
        }
    }

    fun getGutterWidth(): Int {
        return if (getColumnCount() == 1) {
            0
        } else {
            gutterWidth
        }
    }

    /**
     * Default vertical space between items
     */
    fun getGutterHeight(): Int {
        return gutterWidth
    }

    fun getCardDividerPadding(): Int {
        return cardDividerPadding
    }

    fun getSideMargin(): Int {
        val columnCount = getColumnCount()
        return if (columnCount == 1) singleColumnMargin else {
            val contentWidth = columnCount * columnWidth + (columnCount - 1) * gutterWidth // N columns and N-1 gutters in between
            (width - contentWidth) / 2
        }
    }

    fun getContentWidth(): Int {
        val columnCount = getColumnCount()
        if (columnCount == 1) {
            return getColumnWidth()
        }
        return columnCount * columnWidth + (columnCount - 1) * gutterWidth // N columns and N-1 gutters in between
    }

    /**
     * Calculate the total width of units (columns) and gutters between them
     */
    fun getColumnsWidth(units: Int): Int {
        return units * (columnWidth + gutterWidth) - columnWidth
    }

    fun getScreenSizeLayout() = BreakPoints.getScreenSizeLayout(width.toDp(resources.displayMetrics.density))

    fun setShowGrid(showGrid: Boolean) {
        this.drawGrid = showGrid
        invalidate()
    }

    fun setScreenTypeListener(screenTypeListener: ScreenTypeListener) {
        layoutManager?.setScreenTypeListener(screenTypeListener)
    }

    fun releaseResources() {
        adapter?.releaseViewCacheExtensions(this)
        adapter?.releaseCallbackListeners()
        adapter?.clearData()
        adapter = null
        layoutManager = null
        scrollListener?.let {
            removeOnScrollListener(it)
        }
        scrollListener = null
        clearOnScrollListeners()
        recycledViewPool.clear()
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        adapter?.onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(layoutManager?.getScrollPosition() ?: 0, layoutManager?.getGrid()?.checksum, super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            savedState = state
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent?): Boolean {
        when (e?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchdownPoint = PointF(e.x, e.y)
                super.onInterceptTouchEvent(e)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(e.x - touchdownPoint.x)
                val dy = abs(e.y - touchdownPoint.y)
                if (dy > touchSlop && dy > dx) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(e)
    }

    /**
     * Updates the bottom margin of the WPGridView.
     *
     * @param marginPx The new bottom margin in pixels.
     */
    fun updateBottomMargin(marginPx: Int) {
        val layoutParams = this.layoutParams as? MarginLayoutParams
        layoutParams?.let {
            if (it.bottomMargin != marginPx) {
                it.bottomMargin = marginPx
                this.layoutParams = it
            }
        }
    }

    /**
     * Method to get called from the parent views when select any section front pages.
     */
    fun onPageSelected() {
        adapter?.onPageSelected(this)
    }

    class SavedState() : Parcelable {
        var superState: Parcelable? = null
        var scrollPosition: Int = 0
        var checksum: String? = null

        constructor(source: Parcel) : this() {
            val loader = RecyclerView.SavedState::class.java.classLoader
            superState = source.readParcelable(loader)
            scrollPosition = source.readInt()
            checksum = source.readString()
        }

        constructor(scrollPosition: Int, checksum: String?, superState: Parcelable?): this() {
            this.superState = superState
            this.scrollPosition = scrollPosition
            this.checksum = checksum
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(superState, flags)
            dest.writeInt(scrollPosition)
            dest.writeString(checksum)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

    }
}

interface ScreenTypeListener {
    fun onScreenTypeChanged(screenSizeLayout: ScreenSizeLayout)
}

interface GridEnvironment {
    fun getAdViewFactory(): AdViewFactory
    fun isNightModeEnabled(): Boolean
    fun isPhone(): Boolean
    fun isPortraitPhone(): Boolean
    fun openArticle(story: HomepageStory, grid: Grid, sectionDisplayName: String, bundleId: String, position: Int)
    fun openArticleUrl(url: String?, story: HomepageStory, grid: Grid, sectionDisplayName: String, bundleId: String, position: Int)
    fun openMedia(story: HomepageStory, link: Link, grid: Grid, sectionDisplayName: String, bundleId: String)
    fun openRelatedLink(relatedLink: RelatedLinkItem, story: HomepageStory, grid: Grid, sectionDisplayName: String, bundleId: String)
    fun openLabel(label: CompoundLabel, itId: String? = null)
    fun getPager(): SectionsPagerView?
    fun openLiveBlog(link: String, grid: Grid, sectionDisplayName: String, bundleId: String, itId: String?)
    fun getLiveBlogProxyUrl(): String
    fun remoteLogError(tag: String, throwable: Throwable)
    fun isConnected(): Boolean
    fun getCarouselNetworkRequestsHelper(): CarouselProvider
    fun openCarouselCard(links: List<Link>, sectionDisplayName: String, position: Int) {}
    fun openCarouselImmersionCard(links: List<Link>, sectionDisplayName: String, position: Int) {}
    fun openCarouselSevenLiveCard(links: List<Link>, sectionDisplayName: String, position: Int) {}
    fun openCarouselRecipeCard(links: List<Link>, sectionDisplayName: String, position: Int) {}
    fun openStackCard(links: List<Link>, sectionDisplayName: String, position: Int)
    fun openCarouselVideoCard(postTvVideos: List<com.wapo.flagship.features.posttv.model.Video>, grid: Grid, sectionDisplayName: String, position: Int) {}
    fun openCarouselCommentsCard(link: Link, sectionDisplayName: String, position: Int) {}
    fun openCarouselExternalCard(links: List<Link>, sectionDisplayName: String, position: Int) {}
    fun playAudioCarouselAudioArticleItem(carouselAudio: CarouselAudio, position: Int, sectionDisplayName: String?, audioTracker: AudioTracker? = null)
    fun playAudioCarouselAudioPlaylistArticleItem(carouselAudioPlaylist: CarouselAudioPlaylist, playListItems: List<Playlist>, position: Int, sectionDisplayName: String?)
    fun playAudioIfHasAccess(audioMediaConfig: AudioMediaConfig)
    fun generateAudioMediaConfig(carouselAudioItem: CarouselAudioItem?, sectionDisplayName: String?, audioTracker: AudioTracker? = null): AudioMediaConfig?
    fun generateAudioMediaConfig(audioArticle: AudioArticle, isFlexFeature: Boolean, isActionButton: Boolean): AudioMediaConfig
    fun bookMarkClicked(ellipseActionItem: EllipsisActionItem, view: ImageView, isStatusChecked: Boolean)
    fun openHabitTileLink(link : String?, sectionDisplayName: String, trackingSection: String?, trackingSubsection: String?)
    fun trackHabitTileClicked(link: String?)
    fun openWatchVideoCard(postTvVideos: List<com.wapo.flagship.features.posttv.model.Video>, sectionDisplayName: String, tabName: String, position: Int, offset: Int) {}
    fun openForYouVideoCard(video: com.wapo.flagship.features.posttv.model.Video, sectionDisplayName: String) {}
    fun isLowDataModeEnable(): Boolean
    fun isPremiumAccount(): Boolean
    fun openBreakingNewsBar(link: String) {

    }

    fun openLiveVideoBar(link: String) {

    }

    fun onBreakingNewsBarClosed(barEntity: BarEntity) {

    }

    fun shouldShowBreakingNewsBar(barEntity: BarEntity): Boolean {
        return true
    }

    fun getAdTagUrl(video: Video, story: HomepageStory, sectionAdsTargetingContent: TargetingContent?): String?

    fun getSubscribeButton(sectionDisplayName: String?): SubscribeButton?

    fun onRefresh(bundleName: String, sectionDisplayName: String)

    fun getVoteGuideService() : VoteGuideService

    fun onVoteGuideClicked(url: String) {

    }

    fun openLink(url: String) {

    }

    fun setNavigationBehaviorInDefaultMap(navigationBehavior: String) {

    }

    fun provideStackViewAdapter(carousel: Carousel): Adapter? {
        return null
    }

    fun onStackViewCardChanged(carousel: Carousel, pos: Int) {

    }

    fun onCarouselScrollStateChanged(carousel: Carousel, newState: Int) {

    }

    fun getHabitTiles(): List<Tile>

    fun isLoggedInUser(): Boolean

    fun isSaveEnabled(): Boolean

    fun getNewsprintEngagedStatus(): Flow<String>

    fun getNewsprintHasViewed(): Flow<Boolean>

    fun getNewsprintReaderType(): Flow<String>

    fun showSaveRegwall(context: Context)

    fun isAdsContentContextualTargetingEnabled(): Boolean

    fun getSectionInlineMessage(): SectionInlineMessage?

    fun shouldSuppressAds(): Boolean
    fun getPageConfig(): PageConfig
}

interface GridActivity {

    fun getGridEnvironment() : GridEnvironment
}

/**
 * An interface for UI components (like Fragments) that can have their bottom margin dynamically updated.
 */
interface MarginUpdatable {
    /**
     * Updates the bottom margin of a primary view within the component.
     *
     * @param bottomMargin The new bottom margin in pixels.
     */
    fun updateBottomMargin(bottomMargin: Int)
}

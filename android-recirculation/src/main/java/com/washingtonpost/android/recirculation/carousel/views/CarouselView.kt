/*
 * Copyright (c) 2018. The Washington Post
 */
package com.washingtonpost.android.recirculation.carousel.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.AppContextUtils.isTablet
import com.wapo.android.commons.util.ViewUtil.findActivity
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.washingtonpost.android.recirculation.R
import com.washingtonpost.android.recirculation.carousel.adapter.CarouselRecyclerViewAdapter
import com.washingtonpost.android.recirculation.carousel.adapter.VIEW_TYPE_BRIGHT_CARD
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselArrowsClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselClickedListener
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselItemTouchListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselBrightViewItem
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EllipsisActionItem
import com.washingtonpost.android.recirculation.carousel.models.MyPostCarouselViewItem
import kotlin.math.max
import kotlin.math.min

/**
 * Created by adkinsj on 6/21/18.
 */
class CarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val divider: View
    private val sectionName: TextView
    private val recyclerViewContainer: View
    private val backArrow: ImageButton
    private val forwardArrow: ImageButton
    val recyclerView: CarouselRecyclerView
    private val progressView: View
    private var recyclerViewAdapter: CarouselRecyclerViewAdapter? = null
    private val layoutManager: LinearLayoutManager
    private var items: List<CarouselViewItem>? = null
    private var animationDuration = 300
    private var sidePadding = 0
    private var snapHelper: SnapHelper? = null
    private var requestListener: CarouselProvider? = null
    private var clickListener: OnCarouselClickedListener? = null
    private var arrowClickListener: OnCarouselArrowsClickedListener? = null
    private var itemTouchListener: OnCarouselItemTouchListener? = null
    private var consumeTouchEventRule: CarouselConsumeTouchEventRule? = null
    private var fixedCardWidth: Int = -1
    private var fixedCardPadding: Int = -1
    private var onAuthorClick: ((String) -> Unit)? = null
    private var onSaveClick: ((String) -> Unit)? = null
    private var onOptionsClick: ((String) -> Unit)? = null
    private var onEllipsisClick: ((EllipsisActionItem) -> Unit)? = null
    private var getNowPlayingAudioItem: (() -> NowPlayingAudioItem?)? = null
    private var onBookmarkClick: ((EllipsisActionItem, ImageView, isStatusChecked: Boolean) -> Unit)? = null
    private var onAudioIconClicked: ((MyPostCarouselViewItem) -> Unit)? = null
    var onBindListener: ((Int) -> Unit)? = null
        set(value) {
            field = value
            recyclerViewAdapter?.onBindListener = value
        }
    private var shouldDisplayDateTime: Boolean = false
    private var shouldDisplayArrow = false
    private var shouldDisplaySectionName = false
    // to set fixed card height.
    var cardHeight: Int = -1
    private val arrowVisibilityListener = ArrowVisibilityListener()
    private val scrollListener = ScrollListener()
    private var centerView: View? = null
    private var itemsRefresherRunnable: Runnable? = null
    private var isNewsprint: Boolean = false
    private var currentLowDataModeEnableState = false

    @JvmField
    val carouselItemsFetchListener: CarouselItemsFetchListener = object :
        CarouselItemsFetchListener {
        override fun onCarouselItemsFetched(items: List<CarouselViewItem>?) {
            if (items.isNullOrEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                setItems(items)
            }
        }
    }

    @JvmField
    val carouselItemsExcerptListener: CarouselItemsExcerptListener = object :
        CarouselItemsExcerptListener {
        override fun onCarouselExcerptFetched(items: List<CarouselViewItem>?) {
            setExcerptValue(items)
        }
    }


    fun setExcerptValue(items: List<CarouselViewItem>?)
    {
        var isExceptEmpty  = true
        if (items != null) {
            for((index, item) in items.withIndex())
            {
                if (recyclerViewAdapter?.getItemViewType(index) == VIEW_TYPE_BRIGHT_CARD)
                {
                    val brightViewItem: CarouselBrightViewItem = item as CarouselBrightViewItem
                    if (brightViewItem.excerpt?.isNotEmpty() == true)
                    {
                        isExceptEmpty = false
                        break
                    }
                }
            }
        }
        recyclerViewAdapter?.setExcerptValue(isExceptEmpty)
    }


    init {
        View.inflate(context, R.layout.carousel_layout, this)

        recyclerView = findViewById(R.id.rv_carousel)
        recyclerViewContainer = findViewById(R.id.fl_carousel_rv_container)
        forwardArrow = findViewById(R.id.button_carousel_forward_arrow)
        backArrow = findViewById(R.id.button_carousel_back_arrow)
        layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.apply {
            this.layoutManager = this@CarouselView.layoutManager
            onFlingListener = null
            // Separated SnapHelper classes for tablets and phones.
            // CarouselLinearSnapHelper is preforming better for tablets than CarouselSnapHelper
            // w.r.t scrolling.
            snapHelper =
                if (AppContextUtils.isTablet() || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) CarouselLinearSnapHelper() else CarouselSnapHelper()
            snapHelper?.attachToRecyclerView(recyclerView)
            setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING)
            centerView = {
                setCenterView(snapHelper?.findSnapView(layoutManager))
            }
        }
        divider = findViewById(R.id.carousel_view_divider)
        sectionName = findViewById(R.id.tv_carousel_section_name)
        progressView = findViewById(R.id.pb_carousel)
        recyclerView.addOnLayoutChangeListener(arrowVisibilityListener)
        recyclerView.addOnScrollListener(scrollListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initLayout(
        requestsHelper: CarouselProvider?,
        carouseClickedListener: OnCarouselClickedListener?,
        consumeTouchEventRule: CarouselConsumeTouchEventRule?,
        shouldDisplaySectionName: Boolean,
        shouldDisplayArrow: Boolean,
        shouldDisplayDateTime: Boolean,
        fixedCardWidth: Int,
        fixedCardPadding: Int,
        onAuthorClick: ((String) -> Unit)? = null,
        onSaveClick: ((String) -> Unit)? = null,
        onOptionsClick: ((String) -> Unit)? = null,
        onEllipsisClick: ((EllipsisActionItem) -> Unit)? = null,
        onAudioIconClicked: ((MyPostCarouselViewItem) -> Unit)? = null,
        animationDuration: Int = -1,
        marginStart: Int = 0,
        marginEnd: Int = 0,
        onBookmarkClick: ((EllipsisActionItem, ImageView, isStatusChecked: Boolean) -> Unit)? = null,
        isNewsprint: Boolean = false,
        getNowPlayingAudioItem: (() -> NowPlayingAudioItem?)? = null
    ) {
        this.requestListener = requestsHelper
        this.clickListener = carouseClickedListener
        this.consumeTouchEventRule = consumeTouchEventRule
        this.fixedCardWidth = fixedCardWidth
        this.fixedCardPadding = fixedCardPadding
        this.shouldDisplayDateTime = shouldDisplayDateTime
        this.shouldDisplaySectionName = shouldDisplaySectionName
        this.onAuthorClick = onAuthorClick
        this.onSaveClick = onSaveClick
        this.onOptionsClick = onOptionsClick
        this.onEllipsisClick = onEllipsisClick
        this.onBookmarkClick = onBookmarkClick
        this.onAudioIconClicked = onAudioIconClicked
        this.isNewsprint = isNewsprint
        this.shouldDisplayArrow = shouldDisplayArrow
        if (animationDuration > -1) {
            this.animationDuration = animationDuration
        }
        this.getNowPlayingAudioItem = getNowPlayingAudioItem

        if (shouldDisplayArrow) {
            (recyclerView.layoutParams as? MarginLayoutParams)?.apply {
                this.marginStart = marginStart
                this.marginEnd = marginEnd
            }
            val arrowWidth = resources.getDimensionPixelSize(R.dimen.carousel_rv_arrows_width)
            forwardArrow.apply {
                setPadding(0, 0, maxOf(marginEnd - (arrowWidth / 2), 0), 0)
                setOnClickListener {
                    val recyclerPosition = layoutManager.findFirstVisibleItemPosition()
                    layoutManager.scrollToPositionWithOffset(recyclerPosition + 1, 0)
                    arrowClickListener?.onForwardArrowClicked()
                }
            }
            backArrow.apply {
                setPadding(maxOf(marginStart - (arrowWidth / 2), 0), 0, 0, 0)
                setOnClickListener {
                    val recyclerPosition = layoutManager.findFirstVisibleItemPosition()
                    layoutManager.scrollToPositionWithOffset(recyclerPosition - 1, 0)
                    arrowClickListener?.onBackwardArrowClicked()
                }
            }
            updateArrowVisibility()
        } else {
            forwardArrow.visibility = View.GONE
            backArrow.visibility = View.GONE
            (recyclerView.layoutParams as? MarginLayoutParams)?.apply {
                this.marginStart = 0
                this.marginEnd = 0
            }

        }

        showSectionName(shouldDisplaySectionName)
        //pass fixed size to adapter or resize carousel based on screen width
        if (fixedCardWidth > -1) {
            recyclerViewAdapter = CarouselRecyclerViewAdapter(
                fixedCardWidth, cardHeight, requestListener,
                clickListener, consumeTouchEventRule,
                recyclerView,
                shouldDisplayDateTime,
                onAuthorClick, onSaveClick, onOptionsClick, onEllipsisClick, onBookmarkClick,
                onAudioIconClicked,
                isNewsprint,
                {setResizeCarouselView()},
                {restoreSizeCarouselView()},
                getNowPlayingAudioItem
            )
            removeItemDecorations()
            recyclerView.addItemDecoration(PaddedItemDecoration(fixedCardPadding))
            recyclerView.adapter = recyclerViewAdapter
        } else {
            resizeCarouselView()
        }
    }

    fun setInStoryDividers(){
        val divider = MaterialDividerItemDecoration(recyclerView.context,
            DividerItemDecoration.HORIZONTAL)
        divider.isLastItemDecorated = false
        divider.setDividerColorResource(recyclerView.context, R.color.carousel_vertical_divider_color)
        recyclerView.addItemDecoration(divider)
    }

    fun setInStoryCarouselDesign(){
        val articleMargin = if (isTablet()) AppContextUtils.calculateArticleMargin() else 0
        (recyclerView.layoutParams as? MarginLayoutParams)?.apply {
                this.marginStart = articleMargin
                this.marginEnd = articleMargin
        }

        (divider.layoutParams as? MarginLayoutParams)?.apply {
            this.marginStart = articleMargin
            this.marginEnd = articleMargin
        }

        (sectionName.layoutParams as? MarginLayoutParams)?.apply {
            this.marginStart = articleMargin
            this.marginEnd = articleMargin
        }

        if (shouldDisplayArrow){
            val arrowWidth = resources.getDimensionPixelSize(R.dimen.carousel_rv_arrows_width)
            val arrowOffset = maxOf(articleMargin - (arrowWidth / 2), 0)

            backArrow.translationX = arrowOffset.toFloat()
            forwardArrow.translationX = -arrowOffset.toFloat()
        }
    }

    private fun showSectionName(shouldShow: Boolean) {
        sectionName.tag = shouldShow

        if (shouldShow) {
            (recyclerViewContainer.layoutParams as? MarginLayoutParams)?.apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.carousel_rv_top_margin)
            }
            divider.setBackgroundColor(ContextCompat.getColor(context, R.color.story_carousel_divider_color))
            sectionName.setTextColor(ContextCompat.getColor(context, R.color.carousel_section_name_text_color))
        } else {
            divider.visibility = GONE
            sectionName.visibility = GONE
        }
    }

    private fun resizeCarouselView() {
        //calculate width based on percentage of screen
        val recyclerViewContainerLP = recyclerViewContainer.layoutParams
        val cardWidth = measureCardWidth()
        val cardHeight: Int = resources.getDimension(R.dimen.max_card_height).toInt()
        val outValue = TypedValue()
        resources.getValue(R.dimen.carousel_card_margin_multiplier, outValue, true)
        val cardMarginMultiplier = outValue.float
        val cardMargin = (cardWidth * cardMarginMultiplier).toInt()
        recyclerViewContainerLP?.height = cardHeight + cardMargin
        recyclerViewContainer.layoutParams = recyclerViewContainerLP
        recyclerViewAdapter = CarouselRecyclerViewAdapter(
            cardWidth,
            cardHeight,
            requestListener,
            clickListener,
            consumeTouchEventRule,
            recyclerView,
            shouldDisplayDateTime,
            onAuthorClick,
            onSaveClick,
            onOptionsClick,
            onEllipsisClick,
            onBookmarkClick,
            onAudioIconClicked,
            isNewsprint,
            {setResizeCarouselView()},
            {restoreSizeCarouselView()},
            getNowPlayingAudioItem
        )
        removeItemDecorations()
        recyclerView.addItemDecoration(PaddedItemDecoration(cardMargin / 2))
        recyclerView.adapter = recyclerViewAdapter
    }

    fun onNowPlayingAudioItem(nowPlayingAudioItem: NowPlayingAudioItem?) {
        recyclerViewAdapter?.onNowPlayingAudioItem(nowPlayingAudioItem)
    }

    private fun restoreSizeCarouselView() {
        if (currentLowDataModeEnableState) {
            currentLowDataModeEnableState = false

            val recyclerViewContainerLP = recyclerViewContainer.layoutParams
            val cardWidth = measureCardWidth()
            val outValue = TypedValue()
            resources.getValue(R.dimen.carousel_card_margin_multiplier, outValue, true)
            val cardMarginMultiplier = outValue.float
            val cardMargin = (cardWidth * cardMarginMultiplier).toInt()
            val cardHeight: Int = resources.getDimension(R.dimen.max_card_height).toInt()
            val newHeight = cardHeight + cardMargin + resources.getDimension(R.dimen.restor_card_height_low_data_mode_offset).toInt()
            recyclerViewContainerLP?.height = newHeight
            recyclerViewContainer.layoutParams = recyclerViewContainerLP
            val recyclerViewLP = recyclerView.layoutParams
            recyclerViewLP?.height = newHeight
            recyclerView.layoutParams = recyclerViewLP
        }
    }

    private fun setResizeCarouselView() {
        this.visibility = View.GONE
        return

        // This code will be required once we have design for low data mode on carousel view
/*
        if (!currentLowDataModeEnableState) {
            currentLowDataModeEnableState = true

            val recyclerViewContainerLP = recyclerViewContainer.layoutParams
            val cardWidth = measureCardWidth()
            val cardHeight: Int = resources.getDimension(R.dimen.max_card_height).toInt()
            val outValue = TypedValue()
            resources.getValue(R.dimen.carousel_card_margin_multiplier, outValue, true)
            val cardMarginMultiplier = outValue.float
            val cardMargin = (cardWidth * cardMarginMultiplier).toInt()
            val newHeight = cardHeight + cardMargin - resources.getDimension(R.dimen.max_card_height_low_data_mode).toInt()
            recyclerViewContainerLP?.height = newHeight
            recyclerViewContainer.layoutParams = recyclerViewContainerLP
            val recyclerViewLP = recyclerView.layoutParams
            recyclerViewLP?.height = newHeight
            recyclerView.layoutParams = recyclerViewLP
        }
*/
    }

    /* This calculates the width of the cards as percentage of screen width */
    private fun measureCardWidth() : Int {
        var cardWidth: Int
        val cardWidthMultiplier: Float
        sidePadding = (resources.getDimensionPixelSize(R.dimen.carousel_card_width_calculation_container_margin)
                + resources.getDimensionPixelSize(R.dimen.carousel_card_width_calculation_article_margin))

        val size = Point()
        // Initialize size with the actual screen width
        val display = this.findActivity()?.windowManager?.defaultDisplay
        display?.getSize(size)
        if (size.x <= 0) {
            size.x = resources.displayMetrics.widthPixels
        }
        val screenWidth = size.x
        val outValue = TypedValue()
        resources.getValue(R.dimen.carousel_card_width_multiplier, outValue, true)
        cardWidthMultiplier = outValue.float
        cardWidth = (screenWidth * cardWidthMultiplier).toInt()
        val scaleFactor: Float = if (sidePadding != 0) size.x.toFloat() / screenWidth else 1f
        val maxWidth = (resources.getDimension(R.dimen.max_card_width) / scaleFactor).toInt()
        if (cardWidth > maxWidth) {
            cardWidth = maxWidth
        }
        return cardWidth
    }

    fun setItems(items: List<CarouselViewItem>?) {
        if (items != null && !items.isEmpty()) {
            this.items = items
            sectionName.text = items[0].sectionName
            if (shouldDisplayArrow) {
                backArrow.visibility = VISIBLE
                forwardArrow.visibility = VISIBLE
                crossfadeViews(
                    arrayOf(recyclerView, sectionName, backArrow, forwardArrow),
                    arrayOf(progressView),
                    animationDuration
                )
            } else {
                crossfadeViews(
                    arrayOf(recyclerView, sectionName),
                    arrayOf(progressView),
                    animationDuration
                )
            }
            recyclerViewAdapter?.setItems(items)
            recyclerViewAdapter?.notifyDataSetChanged()
        }
    }

    fun setOnCarouselArrowsClickedListener(listener: OnCarouselArrowsClickedListener) {
        this.arrowClickListener = listener
    }

    fun setOnCarouselItemTouchListener(listener: OnCarouselItemTouchListener) {
        this.itemTouchListener = listener
    }

    @Deprecated("") //Should use this method from android-commons when migrated to postkit
    private fun crossfadeViews(
        fadingInViews: Array<View?>?,
        fadingOutViews: Array<View>?,
        animationDuration: Int
    ) {
        if (fadingInViews == null || fadingInViews.isEmpty() || fadingOutViews == null || fadingOutViews.isEmpty()) {
            return
        }
        for (fadingInView in fadingInViews) {
            if (fadingInView == null || fadingInView.tag == false) continue

            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            fadingInView.alpha = 0f
            fadingInView.visibility = View.VISIBLE

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            fadingInView.animate()
                    .alpha(1f)
                    .setDuration(animationDuration.toLong())
                    .setListener(null)
        }
        for (fadingOutView in fadingOutViews) {

            // Animate the loading view to 0% opacity. After the animation ends,
            // set its visibility to GONE as an optimization step (it won't
            // participate in layout passes, etc.)
            fadingOutView.animate()
                    .alpha(0f)
                    .setDuration(animationDuration.toLong())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            fadingOutView.visibility = View.GONE
                        }
                    })
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val recyclerPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
        if (fixedCardWidth == -1) {
            resizeCarouselView()
        }
        setItems(items)
        if (recyclerPosition > 0) {
            recyclerView.scrollToPosition(recyclerPosition)
            post {
                layoutManager.apply {
                    val view = findViewByPosition(recyclerPosition)
                    if (view != null) {
                        val snapDistance = snapHelper?.calculateDistanceToFinalSnap(this, view)
                        if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
                            recyclerView.smoothScrollBy(snapDistance[0], snapDistance[1])
                        }
                    }
                }
            }
        }
    }

    private fun updateArrowVisibility(onBackClicked: Boolean = false) {
        if (!shouldDisplayArrow) {
            return
        }
        layoutManager.let {
            var firstVisible = it.findFirstCompletelyVisibleItemPosition()
            var lastVisible = it.findLastCompletelyVisibleItemPosition()
            if (firstVisible == -1 && lastVisible == -1) {
                //no item is fully visible, so find the partially visible items instead
                firstVisible = it.findFirstVisibleItemPosition()
                lastVisible = it.findLastVisibleItemPosition()
            }
            val lastPosition = it.itemCount - 1
            if (firstVisible == 0 || firstVisible == -1) {
                backArrow.visibility = GONE
            } else {
                backArrow.visibility = VISIBLE
            }
            if (lastVisible == lastPosition && !onBackClicked) {
                forwardArrow.visibility = GONE
            } else {
                forwardArrow.visibility = VISIBLE
            }
        }
    }

    private fun updateSectionName() {
        items?.apply {
            if (isEmpty()) {
                val itemPosition =
                    this@CarouselView.layoutManager.findFirstCompletelyVisibleItemPosition()
                if (itemPosition in indices) {
                    val sectionNameString = this[itemPosition].sectionName
                    val lastSectionName = sectionName.text
                    if (lastSectionName == null || lastSectionName.toString() != sectionNameString) {
                        val animation = AlphaAnimation(1f, 0f)
                        animation.duration = animationDuration.toLong()
                        animation.repeatCount = 1
                        animation.repeatMode = Animation.REVERSE
                        animation.setAnimationListener(object :
                            Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation) {}
                            override fun onAnimationEnd(animation: Animation) {}
                            override fun onAnimationRepeat(animation: Animation) {
                                sectionName.text = sectionNameString
                            }
                        })
                        sectionName.startAnimation(animation)
                    }
                }
            }
        }
    }

    private fun removeItemDecorations() {
        while (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (itemTouchListener != null) {
            itemTouchListener?.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                itemTouchListener?.requestDisallowInterceptTouchEvent(true)
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun setCenterView(view: View?) {
        centerView = view
    }

    private fun updateWrapperClipping() {
        val isTablet = AppContextUtils.isTablet()

        (recyclerViewContainer as? ViewGroup)?.let { wrapper ->
            wrapper.clipChildren = isTablet
            wrapper.clipToPadding = isTablet
        }
    }

    override fun onAttachedToWindow() {
        startItemsRefresherRunnable()
        updateWrapperClipping()
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val context = context
        if (context is Activity && context.isFinishing) {
            //trigger the recyclerView to release and unbind its children
            recyclerView.adapter = null
        }
        releaseItemsRefresherRunnable()
    }

    private fun startItemsRefresherRunnable() {
        releaseItemsRefresherRunnable()
        recyclerView.adapter?.apply {
            var interval: Long = -1
            items?.forEachIndexed { index, item ->
                if (item.refreshInterval != null && item.refreshInterval > -1) {
                    interval = max(interval, item.refreshInterval)
                    notifyItemChanged(index)
                }
            }
            if (interval > -1) {
                itemsRefresherRunnable = Runnable { startItemsRefresherRunnable() }
                recyclerView.postDelayed(itemsRefresherRunnable, interval )
            }
        }
    }

    private fun releaseItemsRefresherRunnable() {
        if (itemsRefresherRunnable != null) {
            recyclerView.removeCallbacks(itemsRefresherRunnable)
            itemsRefresherRunnable = null
        }
    }

    interface CarouselItemsFetchListener {
        fun onCarouselItemsFetched(items: List<CarouselViewItem>?)
    }

    interface CarouselConsumeTouchEventRule {
        fun canCarouselConsumeTouchEvent(): Boolean
    }

    interface CarouselItemsExcerptListener {
        fun onCarouselExcerptFetched(items: List<CarouselViewItem>?)
    }


    private inner class PaddedItemDecoration(private val padding: Int) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            when {
                parent.getChildAdapterPosition(view) == 0 -> {
                    // first item
                    outRect.right = padding
                }
                parent.getChildAdapterPosition(view) == (parent.adapter?.itemCount ?: 0) - 1 -> {
                    // last item
                    outRect.left = padding
                }
                else -> {
                    outRect.right = padding
                    outRect.left = padding
                }
            }
        }

    }

    /**
     * This class helps to scroll carousel items more freely and also fixes end items scroll bug (cut off end items)
     * with PagerSnapHelper.
     */
    class CarouselLinearSnapHelper : LinearSnapHelper() {
        override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
            if (layoutManager is LinearLayoutManager) {
                if (!needToDoSnap(layoutManager)) {
                    return null
                }
            }
            return super.findSnapView(layoutManager)
        }

        private fun needToDoSnap(linearLayoutManager: LinearLayoutManager): Boolean {
            return linearLayoutManager.findFirstCompletelyVisibleItemPosition() != 0
                    && linearLayoutManager.findLastCompletelyVisibleItemPosition() != linearLayoutManager.itemCount - 1
        }
    }

    private inner class CarouselSnapHelper : PagerSnapHelper() {
        private var mHorizontalHelper: OrientationHelper? = null
        override fun calculateDistanceToFinalSnap(
            layoutManager: RecyclerView.LayoutManager,
            targetView: View
        ): IntArray? {
            return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                    (layoutManager.getPosition(targetView) == 0 || layoutManager.getPosition(
                        targetView
                    ) == layoutManager.itemCount - 2)) {
                val out = IntArray(2)
                out[0] = distanceToStart(targetView, getHorizontalHelper(layoutManager))
                out
            } else {
                super.calculateDistanceToFinalSnap(layoutManager, targetView)
            }
        }

        private fun distanceToStart(targetView: View, helper: OrientationHelper): Int {
            return helper.getDecoratedStart(targetView) - helper.startAfterPadding
        }

        private fun getHorizontalHelper(layoutManager: RecyclerView.LayoutManager): OrientationHelper {
            return mHorizontalHelper ?: OrientationHelper.createHorizontalHelper(layoutManager).apply {
                mHorizontalHelper = this
            }
        }

        override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager,
            velocityX: Int,
            velocityY: Int
        ): Int {
            val centerView = centerView ?: return RecyclerView.NO_POSITION
            setCenterView(null)
            val position = layoutManager.getPosition(centerView)
            var targetPosition = -1
            if (layoutManager.canScrollHorizontally()) {
                targetPosition = if (velocityX < 0) {
                    position - 1
                } else {
                    position + 1
                }
            }
            val firstItem = 0
            val lastItem = layoutManager.itemCount - 1
            targetPosition = min(lastItem, max(targetPosition, firstItem))
            val isEndOflist = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == lastItem
            return if (isEndOflist) lastItem else targetPosition
        }
    }

    /**
     * ScrollListener class to handle any RecyclerView's scroll events.
     */
    private inner class ScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (shouldDisplaySectionName) {
                    updateSectionName()
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (shouldDisplayArrow) {
                updateArrowVisibility()
            }
        }
    }

    private inner class ArrowVisibilityListener : OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            updateArrowVisibility()
        }

    }
}
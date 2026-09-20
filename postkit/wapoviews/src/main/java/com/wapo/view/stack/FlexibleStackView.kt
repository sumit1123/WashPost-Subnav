package com.wapo.view.stack

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.database.DataSetObserver
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Adapter
import android.widget.AdapterView
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.UiUtils
import com.wapo.android.commons.util.animateRotate
import com.wapo.view.ParentScroll
import com.wapo.view.R
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


private const val DEFAULT_STACK_MAX_SIZE = 3
private const val DEFAULT_ANIMATION_DURATION = 300
private const val DEFAULT_SWIPE_ROTATION = 20F
private const val DEFAULT_SIZE_REDUCE_FACTOR = 0.07f
private const val DEFAULT_CARD_SIZE_WITH_EXCERPT_RATIO = 1.10f
private const val DEFAULT_CARD_SIZE_RATIO = 0.8f



class FlexibleStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AdapterView<Adapter>(context, attrs, defStyleAttr) {

    private var hasExcerpt: Boolean = false
    private var pendingPosition: Int = -1
    private var emptyViewId: Int
    private var swipeRotation = DEFAULT_SWIPE_ROTATION
    private var animationDuration = DEFAULT_ANIMATION_DURATION
    private var stackSize = DEFAULT_STACK_MAX_SIZE
    private val originalAllParentsClipChildrenConfig = mutableMapOf<Int, Boolean>()
    private val originalAllParentsClipToPaddingConfig = mutableMapOf<Int, Boolean>()
    private var adapter: Adapter? = null
    private var stackCardWidth = 0
    private var stackCardHeight = 0
    private var initialX: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialY: Float = 0f
    private var initialTouchY: Float = 0f
    private var previousTouchX = 0F
    private var previousTouchY = 0F
    private var isAnimating = false
    private var touchDownRegistered = false
    private var prevView: View? = null
    private var cardShift = 0
    private val nudgeAnimatorSet = AnimatorSet()

    private val dataObserver: DataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()

            invalidate()
            requestLayout()
        }
    }

    private enum class TouchIntent {
        PREVIOUS, NEXT
    }

    private var touchIntent: TouchIntent? = null

    var cardSizeRatio = DEFAULT_CARD_SIZE_RATIO
    var cardSizeWithExcerptRatio = DEFAULT_CARD_SIZE_WITH_EXCERPT_RATIO
    var cardSizeReduceFactor: Float = DEFAULT_SIZE_REDUCE_FACTOR

    enum class Mode {
        LOOP, EMPTY_CARD, LAST_CARD
    }

    var mode = Mode.LAST_CARD

    interface CardChangeListener {
        fun onCardChanged(position: Int)
    }

    var cardChangeListener: CardChangeListener? = null
    private var lastReportedPosition = -1


    init {
        isChildrenDrawingOrderEnabled = true

        clipToPadding = false
        clipChildren = false

        isFocusableInTouchMode = true

        val a = context.obtainStyledAttributes(attrs, R.styleable.FlexibleStackView)
        try {
            emptyViewId =
                a.getResourceId(R.styleable.FlexibleStackView_emptyView, R.layout.stack_empty_view)
        } finally {
            a.recycle()
        }
    }

    override fun getAdapter(): Adapter? {
        return adapter
    }

    override fun setAdapter(adapter: Adapter?) {
        this.adapter?.unregisterDataSetObserver(dataObserver)
        this.adapter = adapter
        this.adapter?.registerDataSetObserver(dataObserver)
        removeAllViewsInLayout()
        requestLayout()
    }

    fun setHasExcerpt(hasExcerpt: Boolean) {
        this.hasExcerpt = hasExcerpt
    }

    override fun getSelectedView(): View {
        return getChildAt(0)
    }

    override fun setSelection(position: Int) {
        if (childCount > 0) {
            val currentPos = getChildAt(0).getAdapterPosition()
            if (currentPos != position) {
                pendingPosition = position
                requestLayout()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        stackCardWidth = (measuredWidth * cardSizeRatio).toInt()
        stackCardHeight = if (hasExcerpt) (measuredWidth * cardSizeWithExcerptRatio).toInt() else measuredWidth
        setMeasuredDimension(measuredWidth, stackCardHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isEmpty()) {
            removeAllViewsInLayout()
            addEmptyView()
            return
        }

        if (pendingPosition >= 0) {
            removeAllViewsInLayout()
        }

        getChildAt(0)?.let {
            if (it.alpha < 1) {
                post { requestLayout() }
                return
            }
        }

        val adapter = adapter ?: return

        val lastIndex = if (childCount == 0) {
            -1
        } else {
            getChildAt(childCount - 1).getAdapterPosition()
        }

        val delta = stackSize - childCount

        if (delta < 0 && touchIntent == null) {
            //got more cards than it should be in the stack
            //remove bottom ones
            for (i in 0 until -delta) {
                removeWithAnimation(getChildAt(childCount - 1))
            }
        }

        var index = if (pendingPosition >= 0) pendingPosition else lastIndex + 1
        for (i in 0 until delta) {
            if (mode == Mode.EMPTY_CARD && index >= adapter.count) {
                addEmptyView()
                break
            } else if (mode == Mode.LAST_CARD && index >= adapter.count) {
                break
            } else {
                val adapterPos = index.rem(adapter.count)
                val view = adapter.getView(adapterPos, null, this)
                addItem(view, -1)
                view.setAdapterPosition(adapterPos)
                index++
            }

        }

        if (childCount > 0 && touchIntent == null) {
            transformChildrenWithProgress(0f, true)
        }

        if (touchIntent == null) {
            val currentPosition = getChildAt(0).getAdapterPosition()
            if (lastReportedPosition != currentPosition) {
                cardChangeListener?.onCardChanged(currentPosition)
                lastReportedPosition = currentPosition
            }
        }

        pendingPosition = -1
    }

    private fun addEmptyView() {
        if (childCount > 0) {
            val lastView = getChildAt(childCount - 1)
            if (lastView.getAdapterPosition() == adapter?.count ?: Int.MAX_VALUE) {
                //already added
                return
            }
        }
        val emptyView = LayoutInflater.from(context).inflate(emptyViewId, this, false)
        emptyView.setAdapterPosition(adapter?.count ?: Int.MAX_VALUE)
        addItem(emptyView, -1)
    }

    private fun addItem(childView: View, index: Int = -1, preventRequestLayout: Boolean = true) {

        // Measure child view
        var params = childView.layoutParams
        if (params == null) {
            params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        val measureSpecWidth = MeasureSpec.makeMeasureSpec(stackCardWidth, MeasureSpec.EXACTLY)
        val measureSpecHeight = MeasureSpec.makeMeasureSpec(stackCardHeight, MeasureSpec.EXACTLY)
        childView.measure(measureSpecWidth, measureSpecHeight)

        // Layout child view
        layoutItem(childView)

        // Add child view
        addViewInLayout(childView, index, params, preventRequestLayout)
    }

    private fun layoutItem(childView: View) {
        childView.layout(
            paddingLeft,
            paddingTop,
            paddingLeft + childView.measuredWidth,
            paddingTop + childView.measuredHeight
        )
    }

    fun next() {
        if (isEmpty() || isEndOfStack()) {
            return
        }
        val childView = prevView ?: getChildAt(0)
        animateToOutOfScreen(childView)

        if (childCount > 1) {
            //animate bottom cards
            val cardShift = calculateCardShift(getChildAt(0))
            for (i in 1 until childCount) {
                val view = getChildAt(i)
                if (view != null) {
                    animateToPosition(view, cardShift, i - 1)
                }
            }
        }
    }

    fun back() {
        if (isEmpty()) {
            return
        }
        val adapter = this.adapter ?: return
        val childView = getChildAt(0)
        val adapterPosition = childView.getAdapterPosition()
        if (adapterPosition > 0) {
            removeViewInLayout(getChildAt(childCount - 1))
            val position = adapterPosition - 1
            val view = adapter.getView(position, null, this)
            addItem(view, 0)
            view.setAdapterPosition(position)
            requestLayout()
        }
    }

    fun nudge() {
        val firstCardView = getChildAt(0)
        if (firstCardView == null || firstCardView.getAdapterPosition() != 0) {
            return
        }
        setAllParentsClipConfig()
        val rotateOut = firstCardView.animateRotate(0f, -5f, firstCardView.left.toFloat(), firstCardView.bottom.toFloat(), true, 300, 300)
        val rotateBack = firstCardView.animateRotate(-5f, 0f, firstCardView.left.toFloat(), firstCardView.bottom.toFloat(), false, 300, 0, AccelerateInterpolator())

        nudgeAnimatorSet.playSequentially(rotateOut, rotateBack)
        nudgeAnimatorSet.addListener(object : SimpleAnimationListener() {

            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                isAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                firstCardView.pivotX = 0.5f
                firstCardView.pivotY = 0.5f
                resetAllParentsClipConfig()
                isAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                firstCardView.pivotX = 0.5f
                firstCardView.pivotY = 0.5f
                resetAllParentsClipConfig()
                isAnimating = false
            }
        })
        nudgeAnimatorSet.start()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isEmpty()) {
            return false
        }
        if (isAnimating) {
            requestDisallowInterceptTouchEvent(true)
            return true
        }
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isEnabled) {
                    return false
                }
                nudgeAnimatorSet.cancel()
                requestDisallowInterceptTouchEvent(true)

                touchIntent = null

                val childView = getChildAt(0)
                initialX = childView.x
                initialY = childView.y

                previousTouchX = event.getX(0)
                previousTouchY = event.getY(0)
                initialTouchX = previousTouchX
                initialTouchY = previousTouchY

                setAllParentsClipConfig()

                touchDownRegistered = true

                return true
            }
            MotionEvent.ACTION_MOVE -> {

                if (!touchDownRegistered) {
                    requestDisallowInterceptTouchEvent(true)
                    return true
                }

                val touchX = event.getX(0)
                val touchY = event.getY(0)
                var childView = prevView ?: getChildAt(0)

                if (touchIntent == null) {
                    touchIntent = when {
                        touchX < previousTouchX -> {
                            if (isEndOfStack()) {
                                // no cards left to swipe, return null
                                null
                            } else {
                                TouchIntent.NEXT
                            }
                        }
                        touchX > previousTouchX -> {
                            prevView = addPreviousCard()
                            if (prevView != null) {
                                childView = prevView
                                TouchIntent.PREVIOUS
                            } else {
                                // no previous card, return null
                                null
                            }
                        }
                        else -> {
                            null
                        }
                    }
                }

                when (touchIntent) {
                    TouchIntent.NEXT, TouchIntent.PREVIOUS -> {
                        var newX = childView.x + (touchX - previousTouchX)

                        //don't let the view go more right than it initially was
                        newX = min(newX, initialX)

                        previousTouchX = touchX
                        previousTouchY = touchY

                        childView.x = newX
                        val dragDistanceX = newX - initialX
                        val swipeProgress = max(dragDistanceX / width, -1F).coerceAtMost(1F)

                        if (swipeRotation > 0) {
                            childView.rotation = swipeRotation * swipeProgress
                        }

                        childView.alpha = 1F - abs(swipeProgress).coerceAtMost(1F)

                        transformChildrenWithProgress(swipeProgress, false)
                    }
                    else -> {
                        // no op
                    }
                }

                val dragDistanceY = (touchY - initialTouchY).absoluteValue

                if (dragDistanceY > childView.height / 4) {
                    // if swiped to far horizontally -> just stop and reset
                    requestDisallowInterceptTouchEvent(false)
                } else {
                    requestDisallowInterceptTouchEvent(true)
                }

                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestDisallowInterceptTouchEvent(false)

                if (!touchDownRegistered) {
                    return true
                }

                touchDownRegistered = false

                val touchX = event.getX(0)
                val touchY = event.getY(0)
                val deltaX = (touchX - initialTouchX).absoluteValue
                val deltaY = (touchY - initialTouchY).absoluteValue
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                swipeOrResetViewPosition()
                if (event.actionMasked == MotionEvent.ACTION_UP && deltaX < touchSlop && deltaY < touchSlop) {
                    val adapter = this.adapter
                    if (adapter != null) {
                        for (i in 0 until childCount) {
                            val childView = getChildAt(i)
                            if (UiUtils.isViewHeightFullyVisibleOnScreen(childView)) {
                                val pos = childView.getAdapterPosition()
                                performItemClick(childView, pos, adapter.getItemId(pos))
                                break
                            }
                        }
                    }
                }
                swipeOrResetViewPosition()
                touchIntent = null
                prevView = null
                return true
            }
        }

        return super.onTouchEvent(event)
    }


    private fun transformChildrenWithProgress(swipeProgress: Float, includeTopCard: Boolean) {
        val startIndex = if (includeTopCard) 0 else 1
        if (childCount <= startIndex) return
        val cardShift = calculateCardShift(getChildAt(0))
        for (i in startIndex until childCount) {
            val view = getChildAt(i)
            val position = i + swipeProgress

            val scaleFactor = 1 - cardSizeReduceFactor * position
            val translationFactor = cardShift.toFloat() * position
            view.alpha = 1f
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor
            view.translationX =
                translationFactor + (view.measuredWidth * cardSizeReduceFactor * position / 2)
        }
    }

    private fun addPreviousCard(): View? {
        val adapter = this.adapter ?: return null
        val topChild = getChildAt(0)
        val adapterPosition = topChild.getAdapterPosition()
        if (adapterPosition > 0) {
            val newPos = adapterPosition - 1
            val view = adapter.getView(newPos, null, this)
            view.setAdapterPosition(newPos)
            addItem(view, 0, false)

            view.x = -view.measuredWidth.toFloat()
            view.rotation = -swipeRotation
            view.alpha = 0.1f
            return view
        }
        return null
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        var parent = parent
        while (parent != null) {
            if (parent is ParentScroll) {
                parent.setShouldAllowScroll(!disallowIntercept)
            }
            parent = parent.parent
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    private fun swipeOrResetViewPosition() {
        if (!isEnabled) {
            resetItemPosition()
            return
        }

        val childView = prevView ?: getChildAt(0)
        val threshold = width / 3F
        when {
            touchIntent == TouchIntent.NEXT && (childView.x + childView.width < 2 * threshold) -> {
                next()
            }
            touchIntent == TouchIntent.PREVIOUS && (childView.x + childView.width < threshold) -> {
                next()
            }
            else -> {
                resetItemPosition()
            }
        }
    }

    private fun resetItemPosition() {
        val childView = prevView ?: getChildAt(0)

        childView.animate()
            .x(initialX)
            .y(initialY)
            .rotation(0F)
            .alpha(1F)
            .setDuration(animationDuration.toLong())
            .setInterpolator(OvershootInterpolator(1.4F))
            .setListener(object : SimpleAnimationListener() {
                override fun onAnimationEndOrCancel(animation: Animator) {
                    resetAllParentsClipConfig()
                    requestLayout()
                    isAnimating = false
                }

                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    isAnimating = true
                }
            })

        if (childCount > 1) {
            for (i in 1 until childCount) {
                val view = getChildAt(i)
                animateToPosition(view, calculateCardShift(getChildAt(0)), i)
            }
        }
    }

    private fun setAllParentsClipConfig() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return
        }
        var view: View = this

        while (view.parent != null && view.parent is ViewGroup) {
            val viewGroup = view.parent as ViewGroup

            originalAllParentsClipChildrenConfig[viewGroup.id] = viewGroup.clipChildren

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalAllParentsClipToPaddingConfig[viewGroup.id] = viewGroup.clipToPadding
            }

            viewGroup.clipChildren = false
            viewGroup.clipToPadding = false

            if (viewGroup is RecyclerView) {
                return
            }

            view = viewGroup
        }
    }

    private fun resetAllParentsClipConfig() {
        var view: View = this

        while (view.parent != null && view.parent is ViewGroup) {
            val viewGroup = view.parent as ViewGroup

            viewGroup.clipChildren = originalAllParentsClipChildrenConfig[viewGroup.id] ?: true
            viewGroup.clipToPadding = originalAllParentsClipToPaddingConfig[viewGroup.id] ?: true

            view = viewGroup
        }

        originalAllParentsClipChildrenConfig.clear()
        originalAllParentsClipToPaddingConfig.clear()
    }

    override fun getChildDrawingOrder(childCount: Int, drawingPosition: Int): Int {
        //reverse
        return childCount - drawingPosition - 1
    }

    private fun animateToOutOfScreen(childView: View) {
        childView.animate().cancel()
        childView.animate()
            .x(-width + childView.x)
            .rotation(-swipeRotation)
            .alpha(0f)
            .setDuration(animationDuration.toLong())
            .setInterpolator(LinearOutSlowInInterpolator())
            .setListener(object : SimpleAnimationListener() {

                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    isAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    removeViewInLayout(childView)
                    resetAllParentsClipConfig()
                    requestLayout()
                    isAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    resetAllParentsClipConfig()
                    isAnimating = false
                }
            })
    }

    private fun animateToPosition(view: View, cardShift: Int, position: Int) {
        view.animate().cancel()
        val scaleFactor = 1 - cardSizeReduceFactor * position
        val translationFactor = cardShift.toFloat() * position
        val translationScaled =
            translationFactor + (view.measuredWidth * cardSizeReduceFactor * position / 2)
        view.animate()
            .scaleX(scaleFactor)
            .scaleY(scaleFactor)
            .translationX(translationScaled)
            .setDuration(animationDuration.toLong())
            .setInterpolator(LinearOutSlowInInterpolator())
            .start()
    }

    private fun removeWithAnimation(view: View?) {
        view ?: return
        view.animate()
            .alpha(0f)
            .setListener(object : SimpleAnimationListener() {

                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    isAnimating = true
                }
                override fun onAnimationEndOrCancel(animation: Animator) {
                    removeViewInLayout(view)
                    isAnimating = false
                }
            })
    }

    private fun calculateCardShift(topChild: View): Int {
        if (cardShift > 0) return cardShift
        val spaceLeft = width - paddingLeft - paddingRight - topChild.measuredWidth
        cardShift = spaceLeft / (stackSize - 1)
        return cardShift
    }

    private fun isEndOfStack(): Boolean {
        if (mode == Mode.LOOP) {
            return false
        }
        val adapter = this.adapter ?: return true
        if (childCount > 0) {
            val view = getChildAt(0)
            val adapterPosition = view.getAdapterPosition()
            if (mode == Mode.EMPTY_CARD && adapterPosition >= adapter.count) {
                return true
            }
            if (mode == Mode.LAST_CARD && adapterPosition >= adapter.count - 1) {
                return true
            }
            return false
        }
        return true
    }

    private fun isEmpty(): Boolean {
        return adapter == null || adapter?.isEmpty == true
    }
}

private fun View.getAdapterPosition(): Int {
    return getTag(R.id.stack_view_pos_tag) as Int
}

private fun View.setAdapterPosition(adapterPosition: Int) {
    return setTag(R.id.stack_view_pos_tag, adapterPosition)
}

private open class SimpleAnimationListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {
        onAnimationEndOrCancel(animation)
    }

    override fun onAnimationCancel(animation: Animator) {
        onAnimationEndOrCancel(animation)
    }

    override fun onAnimationRepeat(animation: Animator) {}

    open fun onAnimationEndOrCancel(animation: Animator) {

    }

}
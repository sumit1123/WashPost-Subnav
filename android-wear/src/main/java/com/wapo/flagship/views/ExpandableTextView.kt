/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.washingtonpost.android.R

class ExpandableTextView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    AppCompatTextView(context, attrs, defStyle) {

    private val onExpandListeners: MutableList<OnExpandListener>
    private var expandInterpolator: TimeInterpolator
    private var collapseInterpolator: TimeInterpolator
    val mMaxLines: Int
    private var animationDuration: Long
    private var animating = false

    /**
     * Is this [ExpandableTextView] expanded or not?
     *
     * @return true if expanded, false if collapsed.
     */
    var isExpanded = false
        private set
    private var collapsedHeight = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        // read attributes
        val attributes: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView, defStyle, 0)
        animationDuration = attributes
            .getInt(R.styleable.ExpandableTextView_animation_duration, 750)
            .toLong()
        attributes.recycle()

        // keep the original value of maxLines
        mMaxLines = this.maxLines

        // create bucket of OnExpandListener instances
        onExpandListeners = ArrayList()

        // create default interpolator
        expandInterpolator = AccelerateDecelerateInterpolator()
        collapseInterpolator = AccelerateDecelerateInterpolator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // if this TextView is collapsed and maxLines = 0,
        // than make its height equals to zero
        var mHeightMeasureSpec = heightMeasureSpec
        if (mMaxLines == 0 && !isExpanded && !animating) {
            mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, mHeightMeasureSpec)
    }

    /**
     * Toggle the expanded state of this [ExpandableTextView].
     *
     * @return true if toggled, false otherwise.
     */
    fun toggle(): Boolean {
        return if (isExpanded) collapse() else expand()
    }

    /**
     * Expand this [ExpandableTextView].
     *
     * @return true if expanded, false otherwise.
     */
    fun expand(): Boolean {
        if (!isExpanded && !animating && mMaxLines >= 0) {
            // notify listener
            notifyOnExpand()

            // measure collapsed height
            measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            collapsedHeight = measuredHeight

            // indicate that we are now animating
            animating = true

            // set maxLines to MAX Integer, so we can calculate the expanded height
            maxLines = Int.MAX_VALUE

            // measure expanded height
            measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val expandedHeight: Int = measuredHeight

            // animate from collapsed height to expanded height
            val valueAnimator = ValueAnimator.ofInt(collapsedHeight, expandedHeight)
            valueAnimator.addUpdateListener { animation ->
                height = animation.animatedValue as Int
            }

            // wait for the animation to end
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // reset min & max height (previously set with setHeight() method)
                    maxHeight = Int.MAX_VALUE
                    minHeight = 0

                    // if fully expanded, set height to WRAP_CONTENT, because when rotating the device
                    // the height calculated with this ValueAnimator isn't correct anymore
                    layoutParams = layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }

                    // keep track of current status
                    isExpanded = true
                    animating = false
                }
            })

            // set interpolator
            valueAnimator.interpolator = expandInterpolator

            // start the animation
            valueAnimator
                .setDuration(animationDuration)
                .start()
            return true
        }
        return false
    }

    /**
     * Collapse this [TextView].
     *
     * @return true if collapsed, false otherwise.
     */
    fun collapse(): Boolean {
        if (isExpanded && !animating && mMaxLines >= 0) {
            // notify listener
            notifyOnCollapse()

            // measure expanded height
            val expandedHeight: Int = measuredHeight

            // indicate that we are now animating
            animating = true

            // animate from expanded height to collapsed height
            val valueAnimator = ValueAnimator.ofInt(expandedHeight, collapsedHeight)
            valueAnimator.addUpdateListener { animation ->
                height = animation.animatedValue as Int
            }

            // wait for the animation to end
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // keep track of current status
                    isExpanded = false
                    animating = false

                    // set maxLines back to original value
                    maxLines = mMaxLines

                    // if fully collapsed, set height back to WRAP_CONTENT, because when rotating the device
                    // the height previously calculated with this ValueAnimator isn't correct anymore
                    layoutParams = layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            })

            // set interpolator
            valueAnimator.interpolator = collapseInterpolator

            // start the animation
            valueAnimator
                .setDuration(animationDuration)
                .start()
            return true
        }
        return false
    }

    /**
     * Sets the duration of the expand / collapse animation.
     *
     * @param animationDuration duration in milliseconds.
     */
    fun setAnimationDuration(animationDuration: Long) {
        this.animationDuration = animationDuration
    }

    /**
     * Adds a listener which receives updates about this [ExpandableTextView].
     *
     * @param onExpandListener the listener.
     */
    fun addOnExpandListener(onExpandListener: OnExpandListener) {
        onExpandListeners.add(onExpandListener)
    }

    /**
     * Removes a listener which receives updates about this [ExpandableTextView].
     *
     * @param onExpandListener the listener.
     */
    fun removeOnExpandListener(onExpandListener: OnExpandListener) {
        onExpandListeners.remove(onExpandListener)
    }

    /**
     * Sets a [TimeInterpolator] for expanding and collapsing.
     *
     * @param interpolator the interpolator
     */
    fun setInterpolator(interpolator: TimeInterpolator) {
        expandInterpolator = interpolator
        collapseInterpolator = interpolator
    }

    /**
     * Sets a [TimeInterpolator] for expanding.
     *
     * @param expandInterpolator the interpolator
     */
    fun setExpandInterpolator(expandInterpolator: TimeInterpolator) {
        this.expandInterpolator = expandInterpolator
    }

    /**
     * Returns the current [TimeInterpolator] for expanding.
     *
     * @return the current interpolator, null by default.
     */
    fun getExpandInterpolator(): TimeInterpolator {
        return expandInterpolator
    }

    /**
     * Sets a [TimeInterpolator] for collapsing.
     *
     * @param collapseInterpolator the interpolator
     */
    fun setCollapseInterpolator(collapseInterpolator: TimeInterpolator) {
        this.collapseInterpolator = collapseInterpolator
    }

    /**
     * Returns the current [TimeInterpolator] for collapsing.
     *
     * @return the current interpolator, null by default.
     */
    fun getCollapseInterpolator(): TimeInterpolator {
        return collapseInterpolator
    }

    /**
     * This method will notify the listener about this view being expanded.
     */
    private fun notifyOnCollapse() {
        for (onExpandListener in onExpandListeners) {
            onExpandListener.onCollapse(this)
        }
    }

    /**
     * This method will notify the listener about this view being collapsed.
     */
    private fun notifyOnExpand() {
        for (onExpandListener in onExpandListeners) {
            onExpandListener.onExpand(this)
        }
    }

    /**
     * Interface definition for a callback to be invoked when
     * a [ExpandableTextView] is expanded or collapsed.
     */
    interface OnExpandListener {
        /**
         * The [ExpandableTextView] is being expanded.
         *
         * @param view the textview
         */
        fun onExpand(view: ExpandableTextView)

        /**
         * The [ExpandableTextView] is being collapsed.
         *
         * @param view the textview
         */
        fun onCollapse(view: ExpandableTextView)
    }

    /**
     * Simple implementation of the [OnExpandListener] interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of [OnExpandListener].
     */
    class SimpleOnExpandListener : OnExpandListener {
        override fun onExpand(view: ExpandableTextView) {
            // empty implementation
        }

        override fun onCollapse(view: ExpandableTextView) {
            // empty implementation
        }
    }

}
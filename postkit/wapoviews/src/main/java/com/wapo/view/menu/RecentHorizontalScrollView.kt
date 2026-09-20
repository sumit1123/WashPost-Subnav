package com.wapo.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.HorizontalScrollView
import kotlin.math.abs

class RecentHorizontalScrollView : HorizontalScrollView {

    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var mIsScrolling: Boolean = false
    private var mOriginalX = 0f
    private var mOriginalY = 0f

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_CANCEL || ev.action == MotionEvent.ACTION_UP) {
                mIsScrolling = false
                parent?.requestDisallowInterceptTouchEvent(false)
            } else {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */
        return when (ev?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                setOriginalMotionEvent(ev)
                super.onInterceptTouchEvent(ev)
            }
            // Always handle the case of the touch gesture being complete.
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                // Release the scroll.
                mIsScrolling = false
                // Do not intercept touch event, let the child handle it
                super.onInterceptTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsScrolling) {
                    // We're currently scrolling, so yes, intercept the touch event!
                    true
                } else {

                    // If the user has dragged her finger horizontally more than the touch slop, start the scroll
                    val xDiff: Int = calculateDistanceX(ev)
                    // Touch slop should be calculated using ViewConfiguration constants.
                    if (xDiff > mTouchSlop) {
                        // Start scrolling!
                        mIsScrolling = true
                        true
                    } else {
                        super.onInterceptTouchEvent(ev)
                    }
                }
            }
            else -> {
                // In general, we don't want to intercept touch events. They should be handled by the child view.
                super.onInterceptTouchEvent(ev)
            }
        }
    }

    private fun calculateDistanceX(ev: MotionEvent): Int {
        return abs(mOriginalX - ev.x).toInt()
    }

    private fun setOriginalMotionEvent(ev: MotionEvent) {
        mOriginalX = ev.x
        mOriginalY = ev.y
    }
}
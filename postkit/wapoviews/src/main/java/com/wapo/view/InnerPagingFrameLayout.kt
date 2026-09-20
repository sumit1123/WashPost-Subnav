package com.wapo.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Use this framelayout to prevent outer viewpager from paging when swiping on inner viewpager.
 */
class InnerPagingFrameLayout : FrameLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var lastX = 0f
    private var lastY = 0f

    /**
     * When user swipes in page block outer pager from intercepting touch event.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.x
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                // Only block touch events when pager is moving right lift. Allow vertical scrolling
                val isRightLeft = abs(lastX - ev.x) > abs(lastY - ev.y)
                getParentPager(this)?.requestDisallowInterceptTouchEvent(isRightLeft)
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    /**
     * Recursive function to find outer viewpager.
     */
    private fun getParentPager(view: ViewParent?): ViewPager2? {
        return when (view) {
            is ViewPager2 -> view
            null -> null
            else -> getParentPager(view.parent)
        }
    }

}
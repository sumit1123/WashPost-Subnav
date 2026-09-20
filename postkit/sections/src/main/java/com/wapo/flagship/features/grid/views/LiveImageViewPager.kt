/* Copyright (c) 2020 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.grid.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class LiveImageViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {

    private var mCurrentPagePosition = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mHeightMeasureSpec = heightMeasureSpec
        try {
            val child = getChildAt(mCurrentPagePosition)
            if (child != null) {
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                val h = child.measuredHeight
                mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onMeasure(widthMeasureSpec, mHeightMeasureSpec)
    }

    fun reMeasureCurrentPage(position: Int) {
        mCurrentPagePosition = position
        //reset section pager scroll after swipe has been consumed
        parent.requestDisallowInterceptTouchEvent(false)
        requestLayout()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (hasOnClickListeners()) {
            return true
        }
        //disable section pager scroll and allow this view pager to swipe
        parent.requestDisallowInterceptTouchEvent(event.action == MotionEvent.ACTION_DOWN)
        return super.onInterceptTouchEvent(event)
    }
}
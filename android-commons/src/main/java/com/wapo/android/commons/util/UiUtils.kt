/*
 * Copyright (c) 2019. The Washington Post. All rights reserved.
 */
package com.wapo.android.commons.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import com.wapo.android.commons.logger.R

object UiUtils {
    /**
     *
     * @param fadingInViews the views that will fade in and become visible
     * @param fadingOutViews the views that will fade out and become invisible (View.GONE)
     * @param animationDuration animation duration in milliseconds
     */
    fun crossfadeViews(
        fadingInViews: Array<View?>?,
        fadingOutViews: Array<View?>?,
        animationDuration: Int
    ) {
        if (fadingInViews.isNullOrEmpty()|| fadingOutViews.isNullOrEmpty()) {
            return
        }
        for (fadingInView in fadingInViews) {
            if (fadingInView == null) continue

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
            if (fadingOutView == null) continue

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

    /**
     * Method to verify the given view's rect intersects with a device's screen rect.
     * @param view
     * @return true when view is visible on the screen otherwise false
     */
    fun isViewVisibleOnScreen(view: View?): Boolean {
        if (view == null || !view.isShown) {
            return false
        }
        val metrics = Resources.getSystem().displayMetrics
        val deviceWidth = metrics.widthPixels
        val deviceHeight = metrics.heightPixels
        val viewRect = Rect()
        view.getLocalVisibleRect(viewRect)
        val screenRect = Rect(0, 0, deviceWidth, deviceHeight)
        return viewRect.intersect(screenRect)
    }

    /**
     * Method to verify the given view's height fully intersects with the device's screen rect.
     * @param view
     * @return true when view's height is fully visible on the screen otherwise false
     */
    fun isViewHeightFullyVisibleOnScreen(view: View?): Boolean {
        if (view == null || !view.isShown) {
            return false
        }
        val viewH = view.measuredHeight
        val viewRect = Rect()
        view.getLocalVisibleRect(viewRect)
        val fullHeightVisible = viewRect.top <= 0 && viewRect.bottom >= viewH
        return isViewVisibleOnScreen(view) && fullHeightVisible
    }

    fun calculateVerticalVisiblePercentage(view: View?): Double {
        if (view == null || !view.isShown) {
            return 0.0
        }

        val itemRect = Rect()

        // Check if view is at least partially visible
        val isViewVisible = view.getLocalVisibleRect(itemRect)
        // Get the height of the visible portion.
        val visibleHeight = itemRect.height().toDouble()
        // Get the full height of the view
        val viewHeight = view.measuredHeight

        return if (isViewVisible && viewHeight > 0) {
            visibleHeight / viewHeight * 100
        } else {
            0.0
        }
    }

    fun calculateHorizontalVisiblePercentage(view: View?): Double {
        if (view == null || !view.isShown) {
            return 0.0
        }

        val itemRect = Rect()

        // Check if view is at least partially visible
        val isViewVisible = view.getLocalVisibleRect(itemRect)
        // Get the height of the visible portion.
        val visibleWidth = itemRect.width().toDouble()
        // Get the full height of the view
        val viewWidth = view.measuredWidth

        return if (isViewVisible && viewWidth > 0) {
            visibleWidth / viewWidth * 100
        } else {
            0.0
        }
    }

    fun screenSizeInPx(context: Context): Point {
        val outMetrics = context.resources.displayMetrics
        return Point(outMetrics.widthPixels, outMetrics.heightPixels)
    }

    fun screenSizeInDp(context: Context): Point {
        val outMetrics = context.resources.displayMetrics
        val density = outMetrics.density
        val sizeInPx = screenSizeInPx(context)
        return Point((sizeInPx.x / density).toInt(), (sizeInPx.y / density).toInt())
    }

    fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    @JvmStatic
    fun isPhone(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.is_phone)
    }
}
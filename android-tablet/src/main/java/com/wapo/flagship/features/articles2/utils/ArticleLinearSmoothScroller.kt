package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.util.DisplayMetrics
import androidx.annotation.NonNull
import androidx.recyclerview.widget.LinearSmoothScroller

class ArticleLinearSmoothScroller(
    @NonNull context: Context,
    private val viewPadding: Int = VIEW_PADDING,
    private val milliSecondsPerInch: Float = MILLISECONDS_PER_INCH,
) : LinearSmoothScroller(context) {
    override fun getVerticalSnapPreference(): Int = SNAP_TO_START

    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int,
    ): Int =
        super.calculateDtToFit(
            viewStart - viewPadding,
            viewEnd,
            boxStart,
            boxEnd + viewPadding,
            snapPreference,
        )

    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float = milliSecondsPerInch / displayMetrics.densityDpi

    companion object {
        private const val VIEW_PADDING = 48
        private const val MILLISECONDS_PER_INCH = 100f // default is 25f (bigger = slower)
    }
}

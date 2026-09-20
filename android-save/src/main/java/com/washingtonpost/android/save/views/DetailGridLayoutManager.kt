/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.save.views

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.save.BuildConfig
import com.washingtonpost.android.save.R
import kotlin.math.max

class DetailGridLayoutManager(val context: Context) :
    GridLayoutManager(context, 1) {

    var layoutHasFooter = false
    var layoutHasBanner = false

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (isFullWidth(position)) spanCount else 1
            }
        }
    }

    fun update() {
        spanCount = max(1, getColumnCount())
        debugSizes()
    }

    /**
     * Should return true for Banner, Header, and Footer items
     */
    fun isFullWidth(position: Int): Boolean {
        return position == 0
                || (layoutHasBanner && position == 1)
                || (layoutHasFooter && position == itemCount - 1)
    }

    private fun getColumnCount(): Int {
        return getScreenWidth() / max(1, getColumnWidthPixelSize())
    }

    private fun getScreenWidth(): Int {
        // return smallest width of the device
        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels
        return if (widthPixels < heightPixels) widthPixels else heightPixels
    }

    private fun getColumnWidthPixelSize(): Int {
        return context.resources.getDimensionPixelSize(R.dimen.my_post_tablet_fixed_width_column)
    }

    private fun debugSizes() {
        if (!BuildConfig.DEBUG) {
            return
        }
        val displayInDp = true
        val display = context.resources.displayMetrics
        val density = if (displayInDp) display.density else 1f
        val debugInfo =
            "View wxh(dp)=${(getScreenWidth() / density).toInt()}x${(display.heightPixels / density).toInt()}," +
                    " cardWidth(dp)=${(getColumnWidthPixelSize() / density).toInt()}," +
                    " colCount=${getColumnCount()}," +
                    " Screen wxh(dp)=${(getScreenWidth() / density).toInt()}x${(display.heightPixels / density).toInt()}," +
                    " density=${display.densityDpi}"
        Logger.d(this.javaClass.simpleName, "MyPostDebug: DetailGridLayoutManager, $debugInfo")
    }
}
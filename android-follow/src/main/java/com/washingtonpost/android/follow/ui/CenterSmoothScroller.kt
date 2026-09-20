package com.washingtonpost.android.follow.ui

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

class CenterSmoothScroller(val context: Context, val position: Int, val callback: () -> Unit) : LinearSmoothScroller(context) {
    init {
        targetPosition = position
    }

    override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
        return boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
    }

    override fun onStop() {
        super.onStop()
        callback()
    }
}
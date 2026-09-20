package com.washingtonpost.foryou.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ForYouDecorator(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildAdapterPosition(view)
        val totalSpanCount = getTotalSpanCount(parent)
        if (totalSpanCount == 1) {
            if (position != 0) {
                outRect.top = spacing
            }
        } else {
            outRect.top = if (isInTheFirstRow(position, totalSpanCount)) 0 else spacing
            outRect.left = if (isFirstInRow(position, totalSpanCount)) 0 else spacing
            outRect.right = if (isLastInRow(position, totalSpanCount)) 0 else spacing
            outRect.bottom = spacing
        }

    }

    private fun getTotalSpanCount(parent: RecyclerView): Int {
        val layoutManager = parent.layoutManager
        return if (layoutManager is GridLayoutManager) {
            layoutManager.spanCount
        } else {
            1
        }
    }

    private fun isInTheFirstRow(position: Int, spanCount: Int): Boolean {
        return position < spanCount
    }

    private fun isFirstInRow(position: Int, spanCount: Int): Boolean {
        return position % spanCount == 0
    }

    private fun isLastInRow(position: Int, spanCount: Int): Boolean {
        return isFirstInRow(position + 1, spanCount)
    }
}
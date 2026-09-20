package com.washingtonpost.android.save.decorators

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.views.DetailGridLayoutManager

class SectionsItemDecoration(
    @DimenRes val leftMargin: Int = -1,
    @DimenRes val topMargin: Int = -1,
    @DimenRes val rightMargin: Int = -1,
    @DimenRes val bottomMargin: Int = -1,
    @DimenRes val leftEdgeMargin: Int = -1,
    @DimenRes val topEdgeMargin: Int = -1,
    @DimenRes val rightEdgeMargin: Int = -1,
    @DimenRes val bottomEdgeMargin: Int = -1,
    @DimenRes val gutterWidth: Int = -1,
    val startPosition: Int = 0
) : RecyclerView.ItemDecoration() {

    private val paintContainer = Paint()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemPosition: Int = parent.getChildAdapterPosition(view)

        val gridLayoutManager = parent.layoutManager as? DetailGridLayoutManager

        if (gridLayoutManager?.isFullWidth(itemPosition) == true) {
            return
        }

        val spanSize = gridLayoutManager?.spanSizeLookup?.getSpanSize(itemPosition) ?: 1
        val spanCount = gridLayoutManager?.spanCount ?: 1
        val column = (itemPosition - startPosition) % spanCount
        val isFirstColumn = column == 0
        val isFirstRow = (itemPosition - startPosition) < spanCount
        val isLastColumn = column == spanCount - 1
        val isLastRow = (parent.childCount - itemPosition - startPosition) <= spanCount

        val gutterWidthForOneItem =
            if (gutterWidth > -1) view.resources.getDimensionPixelSize(gutterWidth) / 2 else -1

        outRect.left =
            when {
                leftEdgeMargin > -1 && isFirstColumn -> view.resources.getDimensionPixelSize(
                    leftEdgeMargin
                )
                gutterWidthForOneItem > -1 -> gutterWidthForOneItem
                leftMargin > -1 -> view.resources.getDimensionPixelSize(leftMargin)
                else -> 0
            }

        val top = if (topEdgeMargin > -1 && isFirstRow) topEdgeMargin else topMargin
        outRect.top = if (top > -1) view.resources.getDimensionPixelSize(top) else 0

        outRect.right =
            when {
                rightEdgeMargin > -1 && isLastColumn -> view.resources.getDimensionPixelSize(
                    rightEdgeMargin
                )
                gutterWidthForOneItem > -1 -> gutterWidthForOneItem
                rightMargin > -1 -> view.resources.getDimensionPixelSize(rightMargin)
                else -> 0
            }

        val bottom = if (bottomEdgeMargin > -1 && isLastRow) bottomEdgeMargin else bottomMargin
        outRect.bottom =
            if (bottom > -1) view.resources.getDimensionPixelSize(bottom)
            else 0
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val gridLayoutManager = parent.layoutManager as? DetailGridLayoutManager
        if (gridLayoutManager != null) {
            val res = parent.resources
            paintContainer.color = res.getColor(R.color.my_post_card_bg)
            val spanCount = gridLayoutManager.spanCount
            val leftMargin = res.getDimensionPixelSize(R.dimen.my_post_card_left_margin)
            val bottomMargin = if (bottomMargin > -1) res.getDimensionPixelSize(bottomMargin) else 0
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val itemPosition = parent.getChildAdapterPosition(child)
                if (gridLayoutManager.isFullWidth(itemPosition)) {
                    continue
                }
                val column = (itemPosition - startPosition) % spanCount
                val isFirstColumn = column == 0
                if (isFirstColumn) {
                    c.drawRect(
                        parent.left.toFloat() + leftMargin, child.top.toFloat(),
                        parent.right.toFloat() - leftMargin, child.bottom.toFloat() + bottomMargin,
                        paintContainer
                    )
                }
            }
        }
    }
}
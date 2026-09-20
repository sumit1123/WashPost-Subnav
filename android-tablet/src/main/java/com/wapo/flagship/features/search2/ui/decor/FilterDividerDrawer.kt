package com.wapo.flagship.features.search2.ui.decor

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.decorator.Decorator
import com.wapo.flagship.features.search2.ui.adapter.FilterAdapter
import com.wapo.flagship.features.search2.ui.adapter.Search2Adapter

class FilterDividerDrawer(
    private val gap: Gap,
) : Decorator.ViewHolderDecor {
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val alpha = dividerPaint.alpha

    init {
        dividerPaint.color = gap.color
        dividerPaint.strokeWidth = gap.height.toFloat()
    }

    override fun draw(
        canvas: Canvas,
        view: View,
        recyclerView: RecyclerView,
        state: RecyclerView.State,
    ) {
        val viewHolder = recyclerView.getChildViewHolder(view)
        val nextViewHolder =
            recyclerView.findViewHolderForAdapterPosition(
                viewHolder.bindingAdapterPosition + 1,
            )

        val startX = recyclerView.paddingLeft + gap.paddingStart
        val startY = view.bottom + view.translationY
        val stopX = recyclerView.width - recyclerView.paddingRight - gap.paddingEnd
        val stopY = startY

        dividerPaint.alpha = (view.alpha * alpha).toInt()

        val shouldDivide =
            viewHolder.itemViewType != FilterAdapter.FilterType.UNKNOWN.id &&
                nextViewHolder?.itemViewType == FilterAdapter.FilterType.HEADER.id

        val drawMiddleDivider =
            Rules.checkMiddleRule(gap.rule) && shouldDivide || nextViewHolder?.itemViewType == Search2Adapter.SearchItemType.EXPANDABLE.id

        if (drawMiddleDivider) {
            canvas.drawLine(startX.toFloat(), startY, stopX.toFloat(), stopY, dividerPaint)
        }
    }
}

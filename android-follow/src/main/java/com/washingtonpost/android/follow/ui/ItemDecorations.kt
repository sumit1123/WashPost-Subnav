package com.washingtonpost.android.follow.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.follow.R
import kotlin.math.roundToInt

class AuthorListMarginItemDecoration(val context: Context) : RecyclerView.ItemDecoration() {
    private val margin = context.resources.getDimensionPixelSize(R.dimen.divider_space)
    private val firstItemMargin = context.resources.getDimensionPixelSize(R.dimen.divider_space_first_item)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position: Int = parent.getChildAdapterPosition(view)
        with(outRect) {
            left = if (position == 0) firstItemMargin else margin
        }
    }
}

class ArticleListDividerItemDecoration(context: Context, orientation: Int) : DividerItemDecoration(context, orientation) {
    private val mBounds = Rect()

    init {
        setDrawable(ColorDrawable(ContextCompat.getColor(context, com.wapo.view.R.color.article_list_divider)))
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || drawable == null) {
            return
        }
        canvas.save()
        val left: Int
        val right: Int
        if (parent.clipToPadding) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
            canvas.clipRect(left, parent.paddingTop, right,
                    parent.height - parent.paddingBottom)
        } else {
            left = 0
            right = parent.width
        }
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + child.translationY.roundToInt()
            drawable?.apply {
                val top = bottom - intrinsicHeight
                setBounds(left, top, right, bottom)
                draw(canvas)
            }
        }
        canvas.restore()
    }
}

class ArticleListMarginItemDecoration(
    val context: Context, val orientation: Int,
    @DimenRes dividerSpace: Int = R.dimen.divider_space,
    val columns: Int = 1, val allowEdgeItems: Boolean = false
) : RecyclerView.ItemDecoration() {
    private val margin = context.resources.getDimensionPixelSize(dividerSpace)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val firstCol = (itemPosition % columns == 0)
        val firstRow = (itemPosition < columns)
        val lastCol = (itemPosition % columns == columns - 1)
        val lastRow = itemPosition >= parent.childCount - columns
        with(outRect) {
            when (orientation) {
                RecyclerView.HORIZONTAL -> {
                    if (allowEdgeItems || !firstRow) {
                        top = margin
                    }
                    if (allowEdgeItems || !lastRow) {
                        bottom = margin
                    }
                }
                RecyclerView.VERTICAL -> {
                    if (allowEdgeItems || !firstCol) {
                        left = margin
                    }
                    if (allowEdgeItems || !lastCol) {
                        right = margin
                    }
                }
            }
        }
    }
}

class CardifiedListTopMarginDecoration(
    val context: Context,
    @DimenRes margin: Int = R.dimen.cardified_list_vertical_margin
) : RecyclerView.ItemDecoration() {
    private val margin = context.resources.getDimensionPixelSize(margin)
    private val marginDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.cardified_list_vertical_margin_decoration)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val firstRow = itemPosition == 0
        with(outRect) {
            if (firstRow) {
                top = margin
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val firstItem = parent.findViewHolderForAdapterPosition(0)?.itemView
        firstItem?.let { itemView ->
            marginDrawable?.apply {
                setBounds(parent.left, itemView.top - margin, parent.right, itemView.top)
                draw(canvas)
            }
        }
    }
}

class CardifiedListBottomMarginDecoration(
    val context: Context,
    @DimenRes margin: Int = R.dimen.cardified_list_vertical_margin
) : RecyclerView.ItemDecoration() {
    private val margin = context.resources.getDimensionPixelSize(margin)
    private val marginDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.cardified_list_vertical_margin_decoration)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val lastRow = itemPosition == parent.adapter?.itemCount?.minus(1)
        with(outRect) {
            if (lastRow) {
                bottom = margin
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val lastItem = parent.findViewHolderForAdapterPosition(parent.adapter?.itemCount?.minus(1) ?: -1)?.itemView
        lastItem?.let { itemView ->
            marginDrawable?.apply {
                setBounds(parent.left, itemView.bottom, parent.right, itemView.bottom + margin)
                draw(canvas)
            }
        }
    }
}

class CardifiedListTopMarginDecorationWithPadding(
    val context: Context,
    @DimenRes margin: Int = R.dimen.cardified_list_vertical_margin
) : RecyclerView.ItemDecoration() {
    private var margin = context.resources.getDimensionPixelSize(margin)
    private val marginDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.cardified_list_vertical_margin_decoration)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val firstRow = itemPosition == 0
        with(outRect) {
            if (firstRow) {
                top = margin
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val firstItem = parent.findViewHolderForAdapterPosition(0)?.itemView
        firstItem?.let { itemView ->
            val topPadding = itemView.context?.resources?.getDimension(R.dimen.cardified_list_vertical_padding)?.toInt() ?: 0
            itemView.setPadding(0, topPadding, 0, 0)
            marginDrawable?.apply {
                setBounds(parent.left, itemView.top - margin, parent.right, itemView.top)
                draw(canvas)
            }
        }
    }
}

class CardifiedListBottomMarginDecorationWithPadding(
    val context: Context,
    @DimenRes margin: Int = R.dimen.cardified_list_vertical_margin
) : RecyclerView.ItemDecoration() {
    private val margin = context.resources.getDimensionPixelSize(margin)
    private val marginDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.cardified_list_vertical_margin_decoration)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val lastRow = itemPosition == parent.adapter?.itemCount?.minus(1)
        with(outRect) {
            if (lastRow) {
                bottom = margin
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val lastItem = parent.findViewHolderForAdapterPosition(parent.adapter?.itemCount?.minus(1) ?: -1)?.itemView
        lastItem?.let { itemView ->
            val bottomPadding = itemView.context?.resources?.getDimension(R.dimen.cardified_list_vertical_padding)?.toInt() ?: 0
            itemView.setPadding(0, 0, 0, bottomPadding)
            marginDrawable?.apply {
                setBounds(parent.left, itemView.bottom, parent.right, itemView.bottom + margin)
                draw(canvas)
            }
        }
    }
}
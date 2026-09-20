package com.wapo.flagship.features.articles2.itemdecorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.articles.R

/**
 * This item decoration is used to add margins for the recycler view items for Article content.
 */
class ViewPagerMarginItemDecoration : RecyclerView.ItemDecoration() {
    /**
     * Please refer [RecyclerView.ItemDecoration.getItemOffsets] for more info on this one.
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val resources = view.resources
        with(outRect) {
            resources.getDimensionPixelSize(R.dimen.viewpager_current_item_horizontal_margin).also {
                left = it
                right = it
                top = it
                bottom = it
            }
        }
    }
}
